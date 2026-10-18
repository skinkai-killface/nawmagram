#!/usr/bin/env python3
import json, copy, sys, os

gs_path = "telegram/TMessagesProj/google-services.json"
if not os.path.exists(gs_path):
    print("google-services.json not found, skipping")
    sys.exit(0)

with open(gs_path) as f:
    data = json.load(f)

new_clients = []
for client in data.get("client", []):
    pkg = client.get("client_info", {}).get("android_client_info", {}).get("package_name", "")
    if "org.telegram.messenger" in pkg:
        clone = copy.deepcopy(client)
        new_pkg = pkg.replace("org.telegram.messenger", "org.telegram.nawmagram")
        clone["client_info"]["android_client_info"]["package_name"] = new_pkg
        new_clients.append(clone)

data["client"].extend(new_clients)

with open(gs_path, "w") as f:
    json.dump(data, f, indent=2)

print(f"Added {len(new_clients)} nawmagram entries to google-services.json")
