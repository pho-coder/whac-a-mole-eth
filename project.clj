(defproject rocks.pho.eth/whac-a-mole-eth "0.1.4-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [org.java-websocket/Java-WebSocket "1.3.4"]
                 [com.alibaba/fastjson "1.2.34"]
                 [mount "0.1.11"]
                 [org.clojure/data.json "0.2.6"]
                 [cprop "0.1.10"]
                 [com.jd.bdp.magpie/magpie-utils "0.1.4-SNAPSHOT"]]
  :main ^:skip-aot rocks.pho.eth.whac-a-mole-eth
  :source-paths ["src" "src/clj"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
