#!/usr/bin/env python3
"""
Nawmagram Plugin Packager (.naw)
Packs classes.dex and plugin.json into an official .naw plugin package.
"""

import os
import sys
import json
import zipfile
import argparse

def create_naw_package(output_file, dex_file, plugin_id, name, version, author, description, main_class, assets_dir=None):
    if not os.path.exists(dex_file):
        print(f"Error: DEX file not found: {dex_file}", file=sys.stderr)
        sys.exit(1)

    metadata = {
        "id": plugin_id,
        "name": name,
        "version": version,
        "author": author,
        "description": description,
        "main": main_class
    }

    print(f"📦 Packaging Nawmagram plugin: {output_file}")
    print(f"   ID:          {plugin_id}")
    print(f"   Name:        {name}")
    print(f"   Version:     {version}")
    print(f"   Author:      {author}")
    print(f"   Main Class:  {main_class}")

    with zipfile.ZipFile(output_file, 'w', compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(dex_file, arcname="classes.dex")
        meta_json = json.dumps(metadata, indent=2, ensure_ascii=False)
        zf.writestr("plugin.json", meta_json)

        if assets_dir and os.path.exists(assets_dir):
            for root, _, files in os.walk(assets_dir):
                for file in files:
                    full_path = os.path.join(root, file)
                    rel_path = os.path.relpath(full_path, assets_dir)
                    zf.write(full_path, arcname=os.path.join("assets", rel_path))

    print(f"\n✅ Plugin successfully built: {output_file} ({os.path.getsize(output_file)} bytes)")

def main():
    parser = argparse.ArgumentParser(description="Pack a Nawmagram plugin into .naw format")
    parser.add_argument("--dex", required=True, help="Path to classes.dex")
    parser.add_argument("--id", required=True, help="Unique plugin ID (e.g. org.nawmagram.neon)")
    parser.add_argument("--name", required=True, help="Human-readable plugin name")
    parser.add_argument("--version", default="1.0.0", help="Plugin version")
    parser.add_argument("--author", default="Developer", help="Plugin author")
    parser.add_argument("--desc", default="", help="Plugin description")
    parser.add_argument("--main", required=True, help="Fully qualified main class implementing NawmaPlugin")
    parser.add_argument("--assets", default=None, help="Optional directory with assets/drawables")
    parser.add_argument("-o", "--output", required=True, help="Output .naw file path")

    args = parser.parse_args()
    create_naw_package(
        output_file=args.output,
        dex_file=args.dex,
        plugin_id=args.id,
        name=args.name,
        version=args.version,
        author=args.author,
        description=args.desc,
        main_class=args.main,
        assets_dir=args.assets
    )

if __name__ == "__main__":
    main()
