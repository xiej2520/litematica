{
  description = "A Nix-flake for a minecraft modding dev environment.";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { nixpkgs, ... }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      forEachSupportedSystem =
        f:
        nixpkgs.lib.genAttrs supportedSystems (
          system:
          let
            pkgs = import nixpkgs { inherit system; };
          in
          f pkgs
        );
    in
    {
      devShells = forEachSupportedSystem (
        pkgs:
        let
          lib = pkgs.lib;

          java = pkgs.jetbrains.jdk-no-jcef;

          java8 = pkgs.zulu8.override {
            gtkSupport = false;
          };

          # native libraries the game loads at runtime
          runtimeLibs =
            with pkgs;
            [
              libGL
              libpulseaudio
              flite # TTS for the narrator
            ]
            ++ lib.optionals stdenv.hostPlatform.isLinux [
              glfw3-minecraft # windowing for LWJGL 3 (1.13+), with Minecraft's patches
              alsa-lib # libasound linked by the narrator's flite wrapper
              openal # sound engine for LWJGL 3
              stdenv.cc.cc.lib
              pipewire # PipeWire output for OpenAL
              # LWJGL 2 (1.12.2 and below) and SDL3 on X11 (26.3+)
              libx11
              libxext
              libxcursor
              libxrandr
              libxxf86vm # LWJGL 2 only
              # SDL3 on X11
              libxi
              libxfixes
              # SDL3 on Wayland (26.3+)
              wayland
              libxkbcommon
              libdecor
            ];
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              java
              java8
              gradle_9
            ];
            # gradle_9 uses JAVA_HOME (loom requires 25), prefer ./gradlew
            JAVA_HOME = "${java.home}";
            # Java 8 for testing mods on it, or for projects that run the game on Java 8
            # (`org.gradle.java.installations.fromEnv=JAVA8_HOME` + a Java 8 javaLauncher on the run tasks)
            JAVA8_HOME = "${java8.home}";

            LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath runtimeLibs;
          };
        }
      );
    };
}
