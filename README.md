# Session Recap
A client-side fabric mod that recaps your last session in a world: where you logged off, your last death, what you mined and built, and a note to yourself. It shows:

- when you were last there and how long you played
- where you logged off (click the coordinates to copy them), and how far you are from it now
- your last death location
- the blocks you mined and placed most
- how far you travelled
- a note you left yourself with `/wherewasi note <text>`

Commands: `/wherewasi` (show the recap again), `/wherewasi note [text]`, `/wherewasi clearnote`, `/wherewasi auto on|off`. `/sessionrecap` works as another name for `/wherewasi`, so `/sessionrecap note <text>` and the rest work too.

## FAQ

### Does this need to be installed on the server?

Nope, it runs fully client-sided and works on any server with no installation required server-sided.

### Is it allowed on servers? Is it a cheat?

It only records things you did yourself and could already see. The mod just makes it more seamless and easier to access. It shows nothing about other players.

### What do I need to run it?

Minecraft 26.3, Fabric Loader 0.19.5 or newer, and Fabric API. Older Minecraft versions/Forge and NeoForge may be added if requested.

### Are worlds and servers tracked separately?

Yes. Singleplayer worlds go by save folder name and servers by the address you joined with, so joining the same server by IP and by domain name counts as two different servers.

### Why don't I see a distance next to my death or logoff spot?

It is only shown when you are in the same dimension and more than 16 blocks away.

### I hopped in and out quickly. Why does the recap still show my older session?

This was an intentional feature that I added to prevent a recap from wiping if you join the server to check up really quick on something. If you are in the server for less than 2 minutes without mining or placing blocks, it will not update, only your logoff position.

### Are the mined and placed counts exact?

It is close, but not fully exact since the mod is fully client-sided. A laggy server that rejects a placement may still be counted. Creative mode counts as well.

### Can I turn off the automatic recap?

Yes. Run `/wherewasi auto off` and the recap will no longer show up when you join. Your sessions are still tracked, so you can type `/wherewasi` to see the recap whenever you want. Run `/wherewasi auto on` to turn it back on, or `/wherewasi auto` to check the current setting.