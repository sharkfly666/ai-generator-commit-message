import tempfile
import unittest
from io import BytesIO
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile

from generate_update_plugins import generate


class GenerateUpdatePluginsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.archive = self.root / "plugin.zip"
        self.output = self.root / "updatePlugins.xml"

    def archive_with(self, descriptor):
        jar = BytesIO()
        with ZipFile(jar, "w") as zipped:
            zipped.writestr("META-INF/plugin.xml", descriptor)
        with ZipFile(self.archive, "w") as zipped:
            zipped.writestr("plugin/lib/plugin.jar", jar.getvalue())

    def test_metadata_comes_from_archive(self):
        self.archive_with('<idea-plugin><id>example.plugin</id><version>2.0</version>'
                          '<idea-version since-build="253" until-build="263.*"/></idea-plugin>')
        generate(self.archive, "owner/repo", "v2.0", self.output)
        plugin = ET.parse(self.output).getroot().find("plugin")
        assert plugin is not None
        bounds = plugin.find("idea-version")
        assert bounds is not None
        self.assertEqual(plugin.attrib, {
            "id": "example.plugin", "version": "2.0",
            "url": "https://github.com/owner/repo/releases/download/v2.0/plugin.zip",
        })
        self.assertEqual(bounds.attrib, {"since-build": "253", "until-build": "263.*"})

    def test_tag_mismatch_preserves_existing_index(self):
        self.archive_with('<idea-plugin><id>example.plugin</id><version>2.0</version>'
                          '<idea-version since-build="253"/></idea-plugin>')
        self.output.write_text("existing index")
        with self.assertRaisesRegex(ValueError, "does not match"):
            generate(self.archive, "owner/repo", "v1.0", self.output)
        self.assertEqual(self.output.read_text(), "existing index")

    def test_until_build_can_be_absent(self):
        self.archive_with('<idea-plugin><id>example.plugin</id><version>2.0</version>'
                          '<idea-version since-build="253"/></idea-plugin>')
        generate(self.archive, "owner/repo", "v2.0", self.output)
        bounds = ET.parse(self.output).find("plugin/idea-version")
        assert bounds is not None
        self.assertEqual(bounds.attrib, {"since-build": "253"})

    def test_missing_metadata_is_rejected(self):
        self.archive_with('<idea-plugin><version>2.0</version></idea-plugin>')
        with self.assertRaisesRegex(ValueError, "must include"):
            generate(self.archive, "owner/repo", "v2.0", self.output)
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
