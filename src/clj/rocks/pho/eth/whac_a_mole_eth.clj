(ns rocks.pho.eth.whac-a-mole-eth
  (:gen-class)
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [com.jd.bdp.magpie.util.timer :as timer]
            
            [rocks.pho.eth.watcher :as watcher]
            [rocks.pho.eth.config :refer [env]]))

(mount/defstate data-check-timer
  :start (timer/mk-timer)
  :stop (when @(:active data-check-timer)
          (timer/cancel-timer data-check-timer)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (doseq [component (-> args
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (timer/schedule-recurring data-check-timer 2 5 watcher/data-check)
  (while true
    (Thread/sleep 1000)))
