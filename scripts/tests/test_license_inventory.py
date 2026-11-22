import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("license_inventory", Path(__file__).parents[1] / "audit_release_licenses.py")
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)


class LicenseInventoryTest(unittest.TestCase):
    def test_direct_license_and_parent_are_read_without_executing_properties(self):
        source = b'''<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>Apache-2.0</name><url>https://example.test/license</url></license></licenses><parent><groupId>org.example</groupId><artifactId>parent</artifactId><version>1</version></parent></project>'''
        licenses, parent = AUDIT.parse_pom(source)
        self.assertEqual([{"name": "Apache-2.0", "url": "https://example.test/license"}], licenses)
        self.assertEqual("org.example:parent:1", parent)

    def test_missing_license_is_not_silently_apache(self):
        source = b'<project xmlns="http://maven.apache.org/POM/4.0.0"/>'
        self.assertEqual(([], None), AUDIT.parse_pom(source))

    def test_inherited_license_retains_upstream_notice(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            child = root / "test.group/child/1/hash"
            parent = root / "test.group/parent/1/hash"
            child.mkdir(parents=True)
            parent.mkdir(parents=True)
            child.joinpath("child-1.pom").write_text('''<project xmlns="http://maven.apache.org/POM/4.0.0"><parent><groupId>test.group</groupId><artifactId>parent</artifactId><version>1</version></parent></project>''')
            parent.joinpath("parent-1.pom").write_text('''<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>BSD-3-Clause</name></license></licenses></project>''')
            record = AUDIT.declared_licenses("test.group:child:1", root, root)
            self.assertEqual("test.group:parent:1", record["inherited_from"])
            self.assertEqual("BSD-3-Clause", record["licenses"][0]["name"])

    def test_cyclic_parents_fail_instead_of_guessing(self):
        with self.assertRaises(ValueError):
            AUDIT.declared_licenses("a:b:1", Path("."), Path("."), ("a:b:1",))


if __name__ == "__main__":
    unittest.main()
