(ns rocks.pho.eth.whac-a-mole-eth
  (:gen-class)
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [com.jd.bdp.magpie.util.timer :as timer]
            
            [rocks.pho.eth.watcher :as watcher]
            [rocks.pho.eth.config :refer [env]]))

(mount/defstate data-check-timer
  :start (timer/mk-timer))

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
    (Thread/sleep 10000)
    (log/info "data check timer active:" @(:active data-check-timer))
    (when-not @(:active data-check-timer)
      (log/error "data check timer inactive!")
      (mount/start-with {#'data-check-timer (timer/mk-timer)})
      (timer/schedule-recurring data-check-timer 2 5 watcher/data-check))))
