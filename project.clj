(defproject eisenbeton-client-clj "0.1.0"
  :description "A clojure client lib for handler of an eisenbeton server"
  :url "https://github.com/reshefmann/eisenbeton-client-clj"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [io.nats/jnats "LATEST"]
                 [com.google.flatbuffers/flatbuffers-java "1.12.0"] 
                 [aero/aero "1.1.6"]
                 [com.fulcrologic/guardrails "1.0.0"]
                 [byte-streams "0.2.4"]]
  :plugins  [[lein-shell "0.5.0"]]
  :java-source-paths ["java-src"]
  :source-paths ["src"]

  :aliases {"flatc" ["do" 
                     ["shell" "flatc" "-o" "java-src" "--java" "../eisenbeton-go/flatbuff/request.fbs"]
                     ["shell" "flatc" "-o" "java-src" "--java" "../eisenbeton-go/flatbuff/response.fbs"]]}


  :profiles {:dev {:jvm-opts ["-Dguardrails.enabled"]}}
  :repl-options {:init-ns user})

