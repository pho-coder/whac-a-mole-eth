(ns rocks.pho.eth.whac-a-mole-eth
  (:gen-class)
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]

            [rocks.pho.eth.watcher :as watcher]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (doseq [component (-> args
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (watcher/get-new-depth-data))
