(ns eisenbeton-client-clj.core
  (:require [com.fulcrologic.guardrails.core :refer [>defn >def | ? =>]]
            [byte-streams :as bs])
  (:import [com.google.flatbuffers FlatBufferBuilder]
           [eisenbeton.wire.request EisenRequest]
           [eisenbeton.wire.response EisenResponse Header]
           [io.nats.client Message]))

(set! *warn-on-reflection* true)


;(defn reflect [o] (->> o clojure.reflect/reflect :members (map :name)))

(defn inst-of? [t] (partial instance? t))

(>def ::nats-msg (partial instance? io.nats.client.Message))



 
(>defn extract-flatbuff-msg
  [^Message nats-msg]
  [(inst-of? io.nats.client.Message) => any?]
  (let [buf (java.nio.ByteBuffer/wrap (.getData ^Message nats-msg))
        ^EisenRequest req (eisenbeton.wire.request.EisenRequest/getRootAsEisenRequest buf)]
    {:request req
     :uri (.uri req)
     :path (.path req)
     :method (.method req)
     :content-type (.contentType req)
     :content (.contentAsByteBuffer req)}))

  
(defn build-eisen-response
  [{:response/keys [status headers content]}]
  (let [builder (FlatBufferBuilder. 1024)
        headers (EisenResponse/createHeadersVector 
                  builder
                  (into-array Integer/TYPE
                              (for [[k v] headers] 
                                (do
                                  (Header/createHeader builder (.createString builder ^String k) (.createString builder ^String v))))))
        content (EisenResponse/createContentVector builder ^bytes content)
        builder (doto builder
                  (EisenResponse/startEisenResponse)
                  (EisenResponse/addStatus status)
                  (EisenResponse/addHeaders headers)
                  (EisenResponse/addContent content)
                  (EisenResponse/finishEisenResponseBuffer (EisenResponse/endEisenResponse builder)))]
    (.sizedByteArray builder)))


(defn publish-response
  "Helper method for sending a response to the Eisenbeton server"
  [^io.nats.client.Connection nats-conn 
   reply-inbox 
   ^EisenResponse response]
  (.publish 
    ^io.nats.client.Connection nats-conn
    reply-inbox
    response))


(defn ^io.nats.client.MessageHandler message-handler
  "Wrapper for implementing the nats MessageHandler inteface"
  [nats-conn handler]
  (reify io.nats.client.MessageHandler
    (^void onMessage
      [this ^Message msg]
      (handler 
        (extract-flatbuff-msg msg)
        nats-conn 
        (.getReplyTo msg)))))



(>defn create-nats-conn
  "Create a connection to a nats server/cluster"
  [nats-uris]
  [string? => (inst-of? io.nats.client.Connection)]
  (io.nats.client.Nats/connect ^String nats-uris))


(defn start-listening
  "Connect to nats and start listening for requests for a topic.
  'nats-conn' a connection to nats server/cluster.
  'nats-queue' is a nats queue name for load balancing multiple listeners.
  'nats-topic' is the url path to listen to as the server sends the request with the url path as the topic.
  'handler' is a callback function to actually handle the requests. Should accept: the message received, the nats conn and the reply-to subject in nats.
  Returns a function that calling it will stop listening (stop the dispatcher) after waiting for 3 seconds for draining the messages in flight"
  [^io.nats.client.Connection nats-conn 
   ^String nats-queue 
   ^String nats-topic 
   handler]
  (let [disp (.createDispatcher nats-conn (message-handler nats-conn handler))
        sub (.subscribe disp nats-topic nats-queue)] 
    (fn [] 
      (.drain disp (java.time.Duration/ofMillis 3000))
      (.closeDispatcher nats-conn ^io.nats.client.Dispatcher disp))))






