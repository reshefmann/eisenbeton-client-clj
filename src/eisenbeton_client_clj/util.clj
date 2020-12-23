(ns eisenbeton-client-clj.util)


(defn content-as-byte-array 
  "Takes the request map and returns :content as byte array"
  [req]
  (-> req :content (.array)))

(defn content-as-input-stream
  "Takes the request map and return :content as input stream for efficient use with serializator"
  [req]
  (com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream. (:content req)))
