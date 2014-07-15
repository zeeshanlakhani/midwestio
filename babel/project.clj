(defproject org.zeeshanlakhani/midwestio "0.1.0-SNAPSHOT"
  :description "midwest.io talk - composing test generators"
  :url "https://github.com/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0-RC1"]
                 [org.clojure/test.check "0.5.8"]
                 [prismatic/schema "0.2.4"]
                 [schema-contrib "0.1.3"]
                 [schema-gen "0.1.1-SNAPSHOT"]
                 [net.mikera/core.matrix "0.26.0"]]

  :source-paths ["src/cljx"]
  :test-paths ["target/test-classes"]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2156"]
                                  [criterium "0.4.1"]]
                   :plugins [[com.keminglabs/cljx "0.3.2"]
                             [lein-cljsbuild "1.0.2"]
                             [com.cemerick/clojurescript.test "0.2.2"]
                             [com.cemerick/austin "0.1.4"]]
                   :hooks [cljx.hooks]
                   :aliases {"cleantest" ["do" "clean," "test," "cljsbuild" "test"]}}}

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}
                  {:source-paths ["test/cljx"]
                   :output-path "target/test-classes"
                   :rules :clj}
                  {:source-paths ["test/cljx"]
                   :output-path "target/test-classes"
                   :rules :cljs}]}

  :cljsbuild {:builds [{:source-paths ["target/classes" "target/test-classes"]
                        :id "simple"
                        :compiler {:output-to "target/midwestio-0.1.0-SNAPSHOT.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit-tests" ["phantomjs" :runner "target/midwestio-0.1.0-SNAPSHOT.js"]}}

  :pom-addition [:developers [:developer
                              [:name "Zeeshan Lakhani"]
                              [:url ""]
                              [:timezone "-5"]]])
