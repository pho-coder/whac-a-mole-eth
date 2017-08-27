(ns rocks.pho.eth.watcher
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]

            [rocks.pho.eth.util.utils :as utils]
            [rocks.pho.eth.config :refer [env]])
  (:import [rocks.pho.eth.util WSClient]))


(mount/defstate last-depth-tick-version :start 0)
(mount/defstate last-depth-tick-data :start (hash-set))
(mount/defstate last-trade-detail-tick-id :start 0)
(mount/defstate last-trade-detail-data :start (hash-set))

(defn get-ws-client []
  (let [uri "wss://be.huobi.com/ws"
        depth-topic "market.ethcny.depth.percent10"
        depth-id (rand-nth (range 1000 2000))
        trade-detail-topic "market.ethcny.trade.detail"
        trade-detail-id (rand-nth (range 3000 4000))]
    (WSClient. uri depth-topic depth-id trade-detail-topic trade-detail-id)))

(mount/defstate ws-client :start (get-ws-client))

(defn get-new-data []
  (while (> (.size (.queue ws-client)) 0)
    (let [data-str (.take (.queue ws-client))
          data (json/read-str data-str
                              :key-fn keyword)]
      (utils/log-data data-str)
      (case (:ch data)
        "market.ethcny.depth.percent10" (let [ts (:ts data)
                                              tick-data (:tick data)
                                              version (:version tick-data)]
                                          (if (> version last-depth-tick-version)
                                            (do (mount/start-with {#'last-depth-tick-data (utils/deal-depth-tick-data tick-data ts)})
                                                (mount/start-with {#'last-depth-tick-version version}))
                                            (log/warn "error depth version:" version "last version:" last-depth-tick-version)))
        "market.ethcny.trade.detail" (let [tick-data (:tick data)
                                           id (:id tick-data)]
                                       (if (> id last-trade-detail-tick-id)
                                         (do (doseq [one (:data tick-data)]
                                               (mount/start-with {#'last-trade-detail-data (conj last-trade-detail-data one)}))
                                             (mount/start-with {#'last-trade-detail-tick-id id}))
                                         (log/warn "error trade detail id:" id "last id:" last-trade-detail-tick-id)))
        (log/error data)))))

(defn data-check []
  (get-new-data)
  (log/info  "depth ws client open?:" (.isOpen ws-client)
             "close?:" (.isClosed ws-client)
             "queue size:" (.size (.queue ws-client)))
  (when (or (.isClosed ws-client)
            (not (.isOpen ws-client)))
    (log/error "ws client ERROR!")
    (mount/start-with {#'ws-client (get-ws-client)}))
  (log/info "depth data:" (utils/format-depth-tick-data last-depth-tick-data))
  (mount/start-with {#'last-trade-detail-data (utils/cut-trade-detail (* 5 60 1000)
                                                                      last-trade-detail-data)})
  (let [time-points [10 30 60 120 180]
        trade-detail (utils/deal-trade-detail last-trade-detail-data time-points)]
    (log/info "trade detail data size:" (.size last-trade-detail-data))
    (log/info "trade detail:" (utils/format-deal-trade-detail trade-detail time-points))))
