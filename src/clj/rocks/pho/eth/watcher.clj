(ns rocks.pho.eth.watcher
  (:import [rocks.pho.eth.utils WSClient]))

(defn watch-depth []
  (let [uri "wss://be.huobi.com/ws"
        topic "market.ethcny.depth.step1"
        id 1000
        client (WSClient. uri topic id)]
    (while true
      (prn (.take (.queue client))))))
