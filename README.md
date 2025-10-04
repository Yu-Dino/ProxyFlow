![ProxyFlow](https://i.imgur.com/nfwkxNN.png)
# Das all in one sicherheits system für deinen velocity proxy

ProxyFlow ist ein unverzichtbares Plugin für jeden Minecraft-Serverbetreiber, der einen Velocity-Proxy verwendet. Es kombiniert eine robuste Sicherheitssuite mit einem voll funktionsfähigen Wartungssystem, um deinen Server vor unerwünschten Verbindungen zu schützen und dir die volle Kontrolle zu geben.

Entwickelt für Performance und einfache Bedienung, integriert sich ProxyFlow nahtlos in deine Infrastruktur und bietet sofortigen Schutz nach der Installation.

## Features

**Security System**

Schütze dein Netzwerk proaktiv vor Bots, Angreifern und unerwünschten Spielern, bevor sie überhaupt einen deiner Server erreichen.

VPN & Proxy-Blocker: Verhindert, dass Spieler über anonyme Netzwerke (VPNs oder Proxies) beitreten. Ideal zur Abwehr von Bot-Angriffen und Umgehungen von Sperren. Hat eine bypass permission (Benötigt einen kostenlosen API-Key von proxycheck.io)

Länder-Sperre): Erstelle eine Blacklist, um Spieler aus bestimmten Ländern zu blockieren, oder eine Whitelist, um nur Spieler aus ausgewählten Ländern zuzulassen.

Anti-Bot-Schutz: Erkennt und blockiert verdächtig schnelle Verbindungsversuche und schützt vor ungültigen Spielernamen. Bei wiederholten Verstößen wird die IP-Adresse temporär gesperrt.

**Maintenance System**

![Maintenance motd](https://i.imgur.com/ejimV7c.png)
MOTD: Zeigt eine benutzerdefinierte Nachricht (MOTD) in der Serverliste an, wenn die Wartung aktiv ist. Unterstützt Farbcodes und mehrzeilige Anzeigen.

Spielerkick: Kickt alle Spieler, die nicht die Berechtigung haben, während der Wartung online zu sein.

Bypass-Permission: Vergieb eine spezielle Berechtigung an Teammitglieder, damit diese den Server auch während der Wartungsarbeiten betreten können.

Einfacher Befehl: Schalte den Wartungsmodus mit /maintenance blitzschnell an oder aus.


### Installation & Konfiguration

1. Lade ProxyFlow herunter.

2. Platziere die Datei in den plugins-Ordner deines Velocity-Proxys.

3. Starte den Proxy neu. Eine config.yml wird automatisch erstellt.

4. Gehe auf proxycheck.io, registriere dich, und füg deinen api key in die config ein.

**Konfigurations Datei**
```
maintenance:
  enabled: false
  motd: "&4&lWartungs Arbeiten\n      &lBitte versuche es später erneut."
  kick-message: "&cDer Server befindet sich aktuell im Wartungsmodus."
  bypass-permission: "proxyflow.maintenance.bypass"
security:
  vpn-check:
    enabled: true
    #Hol dir einen API key von https://proxycheck.io
    api-key: "DEIN_API_KEY_HIER"
    bypass-permission: "proxyflow.security.vpn.bypass"
  country-block:
    enabled: false
    mode: "blacklist"
    countries:
      - "example country 1"
      - "example country 2"
      - "example country 3"
´´´
