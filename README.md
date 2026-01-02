![ProxyFlow](https://i.imgur.com/nfwkxNN.png) 
# The All-in-One Security System for your Velocity Proxy

**ProxyFlow** is the ultimate security solution for Minecraft server networks based on Velocity. Version 1.4.0 introduces massive performance improvements through **Asynchronous Processing**, **Caching**, and **Database Support**.

It proactively protects your network from bot attacks, VPN bypasses, and exploits while providing your team with essential tools like a global StaffChat and Discord integration.

**Support & Community**
Questions, bugs, or feature requests? Join our Discord:
[https://discord.gg/DK8ZuTCwEC](https://discord.gg/DK8ZuTCwEC)

---

## Features

### Security System

Protect your network before malicious connections even reach your backend servers.

* **Intelligent VPN & Proxy Blocker:**
* Blocks connections via anonymous networks (VPNs/Proxies).
* **NEW:** Uses an intelligent **Caching System** (stores results for X minutes) to save API limits and prevent join lag.
* Requires a free API key from [proxycheck.io](https://proxycheck.io).


* **Persistent Punishments (SQLite):**
* **NEW:** Temporary bans (e.g., triggered by Anti-Bot) are now stored in a local SQLite database (`database.db`). Bans remain active even after a proxy restart!


* **Country Blocker (GeoIP):** Whitelist or Blacklist specific countries.
* **Anti-Bot & Anti-Exploit:**
* Detects inhumanly fast connection attempts.
* Blocks invalid usernames.
* Prevents packet exploits at the proxy level.


* **Multi-Account Protection:** Limits the number of players allowed to join from the same IP address.

### Discord Integration (NEW)

Connect your proxy to Discord via Webhooks:

* Notifications for **VPN/Bot blocks** (includes IP and username).
* Notifications for **Maintenance Mode** status changes.
* Fully asynchronous – your server will never lag, even if Discord is slow to respond.

### Global StaffChat (NEW)

Communicate with your team across all servers:

* Command: `/sc <message>` or `/staffchat <message>`
* **Toggle Mode:** Use `/sc` without arguments to automatically send all your chat messages to the staff chat.

### Maintenance System

* **Custom MOTD:** Displays maintenance status directly in the server list (supports color codes).
* **Auto-Kick:** Disconnects players without permission during maintenance.
* **Bypass:** Staff members with permission can still join.

### Queue System

* Redirects players to a queue server if the target server (Lobby) is full.
* Priority support for VIPs/Staff.

---

## Installation

1. Download the `ProxyFlow-1.4.0+dependencies.jar`.
2. Place the file into the `plugins` folder of your Velocity Proxy.
3. **Important:** Ensure **Java 21** is installed.
4. Restart the proxy.
5. Add your API key to `config.yml` and configure the Discord Webhooks.

---

## Configuration (config.yml)

```yaml
maintenance:
  enabled: false
  motd: "               &4&lServer is in maintenance!\n    &lPlease try again later"
  kick-message: "&cThis server is currently in maintenance"
  bypass-permission: "proxyflow.maintenance.bypass"

whitelist:
  enabled: false
  kick-message: "&cYou are not whitelisted on this server!"
  players:
    - "Notch"
    - "Dinnerbone"
  bypass-permission: "proxyflow.whitelist.bypass"

queue:
  enabled: false
  max-players: 100
  queue-server: "queue"
  target-server: "lobby"
  queue-message: "&eYou are in queue... Position: &6{position}&e/&6{total}"
  bypass-permission: "proxyflow.queue.bypass"
  priority-permissions:
    "proxyflow.queue.priority.high": 100
    "proxyflow.queue.priority.medium": 50
    "proxyflow.queue.priority.low": 10

security:
  vpn-check:
    enabled: true
    api-key: "YOUR_API_KEY_HERE"
    # How long (in minutes) to cache an IP as "safe"? (Saves API requests)
    cache-duration-minutes: 60
    bypass-permission: "proxyflow.security.vpn.bypass"
    whitelisted-players: []
    block-server-ips: true
    notify-admins: true

  country-block:
    enabled: false
    mode: "blacklist"
    countries:
      - "US"
      - "CN"

  multi-account:
    enabled: true
    bypass-permission: "proxyflow.security.multiaccount.bypass"
    action: "kick"
    kick-message: "&cA player with your IP address is already on this server"
    whitelisted-players: []

discord:
  enabled: false
  webhook-url: "https://discord.com/api/webhooks/..."
  notify-vpn-block: true
  notify-maintenance: true

staffchat:
  enabled: true
  format: "&8[&cSC&8] &e{player}&8: &f{message}"

```

---

## Commands & Permissions

| Command | Permission | Description |
| --- | --- | --- |
| `/proxyflow reload` | `proxyflow.command.reload` | Reloads the configuration. |
| `/maintenance` | `proxyflow.command.maintenance` | Toggles maintenance mode on/off. |
| `/staffchat` | `proxyflow.staffchat` | Sends a message or toggles staff chat. |
| `/whitelist <on/off/add/remove>` | `proxyflow.command.whitelist` | Manages the whitelist. |
| `/security <blockserverips/notify>` | `proxyflow.command.security` | Manages security settings live. |
| **Bypass Permissions** |  |  |
| (See Config) | `proxyflow.maintenance.bypass` | Join during maintenance. |
| (See Config) | `proxyflow.security.vpn.bypass` | Join using a VPN. |
| **Admin Notify** | `proxyflow.notify` | Receive ingame alerts when blocks occur. |
