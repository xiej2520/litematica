{
  description = "A Nix flake for a Minecraft modding dev environment.";

  # make sure to git add the flake
  # $(nix build .#litematica.mitmCache.updateScript --no-link --print-out-paths)
  # nix build
  inputs.nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1.*.tar.gz";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
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
            deps = with pkgs; [
              jdk8 # change java version as necessary
              openjdk
              gradle
              libpulseaudio
              libGL
              glfw
              openal
              stdenv.cc.cc.lib
            ];
          in
          f {
            pkgs = pkgs;
            deps = deps;
            system = system;
          }
        );
    in
    {
      devShells = forEachSupportedSystem (
        { pkgs, deps, ... }:
        {
          default = pkgs.mkShell {
            packages = deps;
            buildInputs = deps;
            LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath deps; # Set up the library path for linking

            # tell Intellij to use jdk and gradle in ./.share since nixos doesn't like dynamically linked executables
            # Settings -> Build, Execution; Deployment -> Build Tools -> Gradle
            shellHook = ''
              export BASE_DIR=$(pwd)
              mkdir -p $BASE_DIR/.share

              if [ -L "$BASE_DIR/.share/java8" ]; then
                unlink "$BASE_DIR/.share/java8"
              fi
              ln -sf ${pkgs.jdk8}/lib/openjdk $BASE_DIR/.share/java8

              if [ -L "$BASE_DIR/.share/java" ]; then
                unlink "$BASE_DIR/.share/java"
              fi
              ln -sf ${pkgs.openjdk}/lib/openjdk $BASE_DIR/.share/java

              if [ -L "$BASE_DIR/.share/gradle" ]; then
                unlink "$BASE_DIR/.share/gradle"
              fi
              ln -sf ${pkgs.gradle}/lib/gradle $BASE_DIR/.share/gradle
              export GRADLE_HOME="$BASE_DIR/.share/gradle"

              export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${pkgs.lib.makeLibraryPath deps};
            '';
          };
        }
      );

      packages = forEachSupportedSystem (
        {
          pkgs,
          deps,
          system,
        }:
        let
          pname = "litematica";
          mcversion = "1.15.2";
          version = "0.0.0-dev+xiej.0";
          minecraft-mod = pkgs.stdenv.mkDerivation (finalAttrs: {
            name = "${pname}-${version}";
            src = ./.;

            nativeBuildInputs = [ pkgs.gradle ];

            # from nixpkgs manual:
            # gradle doesn't provide tools for making dependency resolution reproducible,
            # nixpkgs has mitmCache for intercepting requests and recording them

            # if the package has dependencies, mitmCache must be set
            mitmCache = pkgs.gradle.fetchDeps {
              pkg = finalAttrs.finalPackage;
              data = ./deps.json;
            };

            __darwinAllowLocalNetworking = true; # required on Darwin

            gradleBuildTask = "build";
            gradleFlags = [ "-Dfile.encoding=utf-8" ];

            installPhase = ''
              runHook preInstall
              mkdir -p $out/share
              cp build/libs/*.jar "$out/share/"
              runHook postInstall
            '';

            meta = {
              description = "Minecraft mod built using Gradle and Fabric Loom.";
              platforms = [ system ];
            };
          });
        in
        {
          default = minecraft-mod;
          "${pname}" = minecraft-mod;
        }
      );
    };
}
