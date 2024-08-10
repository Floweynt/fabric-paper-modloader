# Fabric Paper Loader

Enables the use of modern Fabric tooling to mod paper servers, allowing bytecode transformation via Mixins that are 
impossible to achieve with regular paper plugins.

## Building

Run the `build` task (`gradlew build`). The output archive is `fabric-paper-loader-all.jar`.

## Running 

Download or copy the loader jar to the base directory of the server (the one with `paper.jar` or `paperclip.jar`).

Modify startup scripts to launch the loader instead of paperclip.

## Development 

There is no toolchain. The best option currently is to just use paperweight.

### Differences from Fabric 
- Targets paper
- Obfuscated runtime names (no intermediaries)