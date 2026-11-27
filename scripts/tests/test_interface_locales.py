from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
TAGS = ['ru', 'en', 'de', 'pt-BR', 'es', 'fr', 'it', 'pl', 'fi', 'ja']
XML_NAME = '{http://schemas.android.com/apk/res/android}name'
FORMAT = re.compile(r'%\d+\$[ds]')


class InterfaceLocalesTest(unittest.TestCase):
    def test_platform_picker_and_in_app_picker_match(self):
        names = [el.get(XML_NAME) for el in ET.parse(ROOT / 'app/src/main/res/xml/supported_locales.xml').getroot()]
        self.assertEqual(TAGS, names)
        locales = (ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/locale/PawchanLocales.kt').read_text(encoding='utf-8')
        self.assertEqual(TAGS, re.findall(r'"([\w-]+)"', next(x for x in locales.splitlines() if 'val tags =' in x)))

    def test_application_bundle_ships_every_picker_language_together(self):
        build = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        active = re.sub(r'(?m)//[^\n]*|/\*[\s\S]*?\*/', '', build)
        self.assertRegex(active, r'\bbundle\s*\{\s*language\s*\{\s*enableSplit\s*=\s*false\s*\}\s*\}')
        self.assertNotRegex(active, r'(?:localeFilters|resConfigs|resourceConfigurations)\s*(?:\(|[+=])')

    def test_application_owns_runtime_locale_changes_and_pip_uses_its_host(self):
        app = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan'
        controller = app / 'locale/PawchanLocales.kt'
        library_switchers = [p for folder in ('core/ui', 'core/media')
                             for p in (ROOT / folder / 'src/main/kotlin').rglob('*.kt')
                             if re.search(r'\.(?:setLocale|setLocales)\s*\(', p.read_text(encoding='utf-8'))]
        self.assertEqual([], library_switchers)
        self.assertIn('config.setLocale(', controller.read_text(encoding='utf-8'))
        self.assertIn('PawchanLocales.attach(base)', (app / 'MainActivity.kt').read_text(encoding='utf-8'))
        self.assertIn('PawchanLocales.attach(base)', (app / 'PawchanApplication.kt').read_text(encoding='utf-8'))
        media = ROOT / 'core/media/src/main/kotlin/com/solthariel/pawchan/media/video'
        self.assertIn('base.localizedForMedia()', (media / 'LocalizedPipActivity.kt').read_text(encoding='utf-8'))
        self.assertIn('applicationContext as? MediaLocaleHost', (media / 'MediaLocaleHost.kt').read_text(encoding='utf-8'))

    def test_all_localized_resources_have_matching_format_arguments(self):
        for folder in ROOT.glob('**/src/main/res/values'):
            if not (folder / 'ui_labels.xml').exists() and not (folder / 'strings.xml').exists():
                continue
            originals = {e.get('name'): e for f in folder.glob('*.xml') for e in ET.parse(f).getroot()
                         if e.tag in ('string', 'plurals') and e.get('translatable') != 'false'}
            for tag in TAGS[1:]:
                target = folder.parent / ('values-' + tag.replace('pt-BR', 'pt-rBR')) / 'strings.xml'
                translations = {e.get('name'): e for f in target.parent.glob('*.xml') for e in ET.parse(f).getroot()
                                if e.tag in ('string', 'plurals') and e.get('translatable') != 'false'}
                self.assertEqual(originals.keys(), translations.keys(), str(target))
                for key, el in translations.items():
                    source = originals[key]
                    self.assertEqual(source.tag, el.tag)
                    for item in list(el) if el.tag == 'plurals' else [el]:
                        ref = source.find("item[@quantity='%s']" % item.get('quantity')) if el.tag == 'plurals' else source
                        self.assertEqual(sorted(FORMAT.findall(ref.text or '')), sorted(FORMAT.findall(item.text or '')), key)
                        self.assertTrue((item.text or '').strip(), key)
