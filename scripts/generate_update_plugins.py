"""Generate a JetBrains custom repository index from the released plugin ZIP."""

import argparse
from io import BytesIO
from pathlib import Path
from urllib.parse import quote
from xml.etree import ElementTree as ET
from zipfile import ZipFile


def generate(archive: Path, repository: str, tag: str, output: Path) -> None:
    descriptors = []
    with ZipFile(archive) as plugin_zip:
        for name in plugin_zip.namelist():
            if name.endswith(".jar"):
                with ZipFile(BytesIO(plugin_zip.read(name))) as jar:
                    if "META-INF/plugin.xml" in jar.namelist():
                        descriptors.append(ET.fromstring(jar.read("META-INF/plugin.xml")))
    if len(descriptors) != 1:
        raise ValueError("Expected exactly one plugin descriptor in the ZIP")

    descriptor = descriptors[0]
    plugin_id = descriptor.findtext("id")
    version = descriptor.findtext("version")
    idea_version = descriptor.find("idea-version")
    if not plugin_id or not version or idea_version is None or not idea_version.get("since-build"):
        raise ValueError("Plugin descriptor must include id, version and since-build")
    if tag != f"v{version}":
        raise ValueError(f"Release tag {tag!r} does not match plugin version {version!r}")

    url = f"https://github.com/{repository}/releases/download/{quote(tag, safe='')}/{quote(archive.name, safe='')}"
    root = ET.Element("plugins")
    plugin = ET.SubElement(root, "plugin", id=plugin_id, version=version, url=url)
    bounds = {key: idea_version.get(key) for key in ("since-build", "until-build") if idea_version.get(key)}
    ET.SubElement(plugin, "idea-version", bounds)
    ET.indent(root, space="    ")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(ET.tostring(root, encoding="UTF-8", xml_declaration=True) + bytes([10]))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("repository", help="GitHub owner/repo")
    parser.add_argument("tag", help="Release tag, for example v1.2.1")
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    generate(args.archive, args.repository, args.tag, args.output)
