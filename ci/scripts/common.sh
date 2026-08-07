source /opt/concourse-java.sh
export TERM=dumb
setup_symlinks

# Route Gradle dependency resolution through an internal Artifactory mirror to
# avoid Maven Central rate limiting. No-op unless ARTIFACTORY_VIRTUAL_REPO_URL
# is set (e.g. via CI params), so local/external builds are unaffected.
setup_gradle_mirror() {
	[[ -n "${ARTIFACTORY_VIRTUAL_REPO_URL:-}" ]] || return 0

	local init_dir="${GRADLE_USER_HOME:-$HOME/.gradle}/init.d"
	mkdir -p "$init_dir"
	cat >"$init_dir/artifactory-mirror.gradle" <<'EOF'
def mirrorUrl = System.getenv("ARTIFACTORY_VIRTUAL_REPO_URL")
if (mirrorUrl) {
	allprojects {
		def addMirror = { repositories ->
			repositories.maven {
				url = mirrorUrl
				credentials {
					username = System.getenv("ARTIFACTORY_USERNAME")
					password = System.getenv("ARTIFACTORY_PASSWORD")
				}
			}
		}
		buildscript {
			addMirror(repositories)
		}
		addMirror(repositories)
	}
}
EOF
}
setup_gradle_mirror
