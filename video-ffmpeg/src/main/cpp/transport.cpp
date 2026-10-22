#include "transport.hpp"
#include <algorithm>
#include <cerrno>
#include <cstring>
#include <limits>
extern "C" {
#include <libavutil/mem.h>
#include <libavutil/error.h>
}
namespace paw {
Input::Input(JNIEnv* env, jobject reader) : env_(env), reader_(reader) {
    jclass type = env->GetObjectClass(reader);
    read_ = env->GetMethodID(type, "read", "(J[BI)I");
    length_ = env->GetMethodID(type, "length", "()J"); env->DeleteLocalRef(type);
    jbyteArray local = env->NewByteArray(32768);
    buffer_ = local ? static_cast<jbyteArray>(env->NewGlobalRef(local)) : nullptr;
    if (local) env->DeleteLocalRef(local);
    auto* bytes = static_cast<uint8_t*>(av_malloc(32768));
    if (bytes && buffer_ && read_ && length_)
        io = avio_alloc_context(bytes, 32768, 0, this, read, nullptr, seek);
    if (!io) av_free(bytes);
}
Input::~Input() {
    if (io) { av_freep(&io->buffer); avio_context_free(&io); }
    if (buffer_) env_->DeleteGlobalRef(buffer_);
}
int Input::read(void* opaque, uint8_t* data, int count) {
    auto& self = *static_cast<Input*>(opaque);
    int size = self.env_->CallIntMethod(self.reader_, self.read_, self.position_, self.buffer_, std::min(count, 32768));
    if (self.env_->ExceptionCheck()) { self.env_->ExceptionClear(); return AVERROR(EIO); }
    if (size == -1) return AVERROR_EOF;
    if (size <= 0 || size > std::min(count, 32768)) return AVERROR_EXIT;
    self.env_->GetByteArrayRegion(self.buffer_, 0, size, reinterpret_cast<jbyte*>(data));
    self.position_ += size;
    return size;
}
int64_t Input::seek(void* opaque, int64_t offset, int mode) {
    auto& self = *static_cast<Input*>(opaque);
    int64_t length = self.env_->CallLongMethod(self.reader_, self.length_);
    if (self.env_->ExceptionCheck()) { self.env_->ExceptionClear(); return AVERROR(EIO); }
    if (mode & AVSEEK_SIZE) return length >= 0 ? length : AVERROR(ENOSYS);
    mode &= ~AVSEEK_FORCE;
    int64_t base;
    if (mode == SEEK_SET) base = 0;
    else if (mode == SEEK_CUR) base = self.position_;
    else if (mode == SEEK_END && length >= 0) base = length;
    else return AVERROR(EINVAL);
    if ((offset > 0 && base > INT64_MAX - offset) || (offset < 0 && offset < -base)) return AVERROR(EINVAL);
    int64_t next = base + offset;
    if (next < 0 || (length >= 0 && next > length)) return AVERROR(EINVAL);
    self.position_ = next;
    return next;
}
}
