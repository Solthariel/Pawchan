#include "timeline.hpp"
#include <cassert>
int main() {
    paw::Timeline time;
    assert(time.position(100) == 0);
    time.running(true, 100); assert(time.position(1100) == 1000);
    time.running(false, 1100); assert(time.position(9100) == 1000);
    time.speed(2000, 9100); assert(time.position(10100) == 1000);
    time.running(true, 10100); assert(time.position(11100) == 3000);
    time.speed(500, 11100); assert(time.position(13100) == 4000);
    time.anchor(50000, 13100); assert(time.position(15100) == 51000);
    time.running(false, 15100); assert(time.position(1) == 51000);
}
