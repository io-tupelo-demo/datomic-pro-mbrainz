(ns tst.tupelo-datomic.basic
  (:use tupelo.core
        tupelo.test)
  (:require
    [datomic.api :as d]
    [tupelo.profile :as prof]
    ))

(def datomic-uri "datomic:dev://localhost:4334/mbrainz-1968-1973") ; the URI for our music db
(def conn (d/connect datomic-uri))
(def ddb (d/db conn))

; same as last query above but uses "map form" of query syntax
(verify
  (let [r1 (d/q '{:find  [?release-name]
                  :in    [$ ?artist-name]
                  :where [[?artist :artist/name ?artist-name]
                          [?release :release/artists ?artist]
                          [?release :release/name ?release-name]
                          ]}
             ddb "John Lennon")]
    ; note that result of query is a HashSet, even though it prints like a Clojure vector
    (is= (type r1)  java.util.HashSet)
    (is-set= r1
      [["Power to the People"]
       ["Unfinished Music No. 2: Life With the Lions"]
       ["Live Jam"]
       ["Live Peace in Toronto 1969"]
       ["Some Time in New York City"]
       ["Mother"]
       ["Woman Is the Nigger of the World"]
       ["John Lennon/Plastic Ono Band"]
       ["Mind Games"]
       ["Unfinished Music No. 3: Wedding Album"]
       ["Imagine"]
       ["Happy Xmas (War Is Over)"]]))

  ; 4 different ways of writing the same query
  (let [r1 (d/query {:query '[:find (pull ?e pattern)
                              :in $ ?name pattern
                              :where [?e :artist/name ?name]]
                     :args  [ddb "The Beatles" [:artist/startYear :artist/endYear]]
                     })
        r2 (d/query {:query '{:find  [(pull ?e pattern)]
                              :in    [$ ?name pattern]
                              :where [[?e :artist/name ?name]]}
                     :args  [ddb "The Beatles" [:artist/startYear :artist/endYear]]
                     })
        r3 (d/q '[:find (pull ?e pattern)
                  :in $ ?name pattern
                  :where [?e :artist/name ?name]]
             ddb "The Beatles" [:artist/startYear :artist/endYear])
        r4 (d/q '{:find  [(pull ?e pattern)]
                  :in    [$ ?name pattern]
                  :where [[?e :artist/name ?name]]}
             ddb "The Beatles" [:artist/startYear :artist/endYear])
        ]
    (is= r1 r2 r3 r4
      [[#:artist{:endYear 1970, :startYear 1957}]]))

  (let [r1 (d/q '{:find  [?start-year ?end-year]
                  :in    [$ ?name]
                  :where [
                          [?e :artist/name ?name]
                          [?e :artist/startYear ?start-year]
                          [?e :artist/endYear ?end-year]]}
             ddb "The Beatles")]
  ; must use set as outer collection
  (is= r1 #{[1957 1970]}))

  (let [r2 (d/query {:query '{:find  [(pull ?e pattern)]
                              :in    [$ ?name pattern]
                              :where [[?e :artist/name ?name]]}
                     :args  [ddb "Led Zeppelin" [:artist/startYear :artist/endYear]]})]
    (is-set= r2 [[#:artist{:startYear 1968, :endYear 1980}]]))

  )

