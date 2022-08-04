;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; 2022-8-3 modified from the original to show results
(ns tst.tupelo-datomic.pull
  (:use tupelo.core
        tupelo.test)
  (:require
    [datomic.api :as d]))

(def datomic-uri "datomic:dev://localhost:4334/mbrainz-1968-1973") ; the URI for our music db
(def conn (d/connect datomic-uri))
(def db (d/db conn))

; entities used in pull examples
(def led-zeppelin [:artist/gid #uuid "678d88b2-87b0-403b-b63d-5da7465aecc3"])
(def mccartney [:artist/gid #uuid "ba550d0e-adac-4864-b88b-407cab5e76af"])
(def dark-side-of-the-moon [:release/gid #uuid "24824319-9bb8-3d1e-a2c5-b8b864dafd1b"])
(def dylan-harrison-sessions [:release/gid #uuid "67bbc160-ac45-4caf-baae-a7e9f5180429"])

; These are 2 examples of a Datomic EID (i.e. a large positive integer)
(def dylan-harrison-cd (d/q '[:find ?medium .
                              :in $ ?release
                              :where
                              [?release :release/media ?medium]]
                         db
                         (java.util.ArrayList. dylan-harrison-sessions)))

(def ghost-riders (d/q '[:find ?track .
                         :in $ ?release ?trackno
                         :where
                         [?release :release/media ?medium]
                         [?medium :medium/tracks ?track]
                         [?track :track/position ?trackno]]
                    db
                    dylan-harrison-sessions
                    11))

(def concert-for-bangla-desh [:release/gid #uuid "f3bdff34-9a85-4adc-a014-922eef9cdaa5"])

(verify
  (is (int? dylan-harrison-cd)) ; an EID, like 1002754604675937
  (is (int? ghost-riders))

  ; attribute name
  (is= (d/pull db [:artist/name :artist/startYear] led-zeppelin)
    #:artist{:name "Led Zeppelin", :startYear 1968})
  (is (wild-match? #:artist{:country {:db/id :*}} ; :db/id is the keyword for EID
        (d/pull db [:artist/country] led-zeppelin)))

  ; reverse lookup
  (let [r1 (only (d/pull db [:artist/_country] :country/GB))
        r2 (first r1)
        r3 (second r1)
        ]
    ; r1 looks like:
    ;     [:artist/_country
    ;      [#:db{:id 527765581342226}
    ;       #:db{:id 527765581342413}
    ;       #:db{:id 527765581343048}
    ;       #:db{:id 527765581343688}
    ;       #:db{:id 527765581343813}
    ;       #:db{:id 527765581344552}
    ;       #:db{:id 527765581345328}
    ;       #:db{:id 527765581345603}
    ;       #:db{:id 527765581345771}
    ;       ...]]
    (is= r2 :artist/_country)
    (is= (count r3) 482))

  ; component defaults
  ; (spyx-pretty (d/pull db [:release/media] dark-side-of-the-moon))


  ; map specifications. For each :track/artists value, do a sub-pull of :db/id and :artist/name
  (let [res (d/pull db [:track/name
                        {:track/artists [:db/id :artist/name]}] ghost-riders)]
    (is (wild-match? {:track/name    "Ghost Riders in the Sky"
                      :track/artists [{:artist/name "George Harrison" :db/id :*}
                                      {:artist/name "Bob Dylan" :db/id :*}]}
          res)))
  )
(comment
  ; noncomponent defaults (same example as "reverse lookup")
  (d/pull db [:artist/_country] :country/GB)

  ; reverse component lookup
  (d/pull db [:release/_media] dylan-harrison-cd)

  ; nested map specifications
  (d/pull db
    [{:release/media
      [{:medium/tracks
        [:track/name {:track/artists [:artist/name]}]}]}]
    concert-for-bangla-desh)

  ; wildcard specification
  (d/pull db '[*] concert-for-bangla-desh)

  ; wildcard + map specification
  (d/pull db '[* {:track/artists [:artist/name]}] ghost-riders)

  ; default option
  (d/pull db '[:artist/name (:artist/endYear :default 0)] mccartney)

  ; default option with different type
  (d/pull db '[:artist/name (:artist/endYear :default "N/A")] mccartney)

  ; absent attributes are omitted from results
  (d/pull db '[:artist/name :died-in-1966?] mccartney)

  ; explicit limit
  (d/pull db '[(:track/_artists :limit 10)] led-zeppelin)

  ; limit + subspec
  (d/pull db '[{(:track/_artists :limit 10) [:track/name]}]
    led-zeppelin)

  ; limit + subspec + :as option
  (d/pull db '[{(:track/_artists :limit 10 :as "Tracks") [:track/name]}]
    led-zeppelin)

  ; no limit
  (d/pull db '[(:track/_artists :limit nil)] led-zeppelin)

  ; empty results
  (d/pull db '[:penguins] led-zeppelin)

  ; empty results in a collection
  (d/pull db '[{:track/artists [:penguins]}] ghost-riders)


  ; Examples below follow http://docs.datomic.com/query.html#pull

  ; pull expression in query
  (d/q '[:find [(pull ?e [:release/name]) ...]
         :in $ ?artist
         :where [?e :release/artists ?artist]]
    db
    led-zeppelin)

  ; dynamic pattern input
  (d/q '[:find [(pull ?e pattern) ...]
         :in $ ?artist pattern
         :where [?e :release/artists ?artist]]
    db
    led-zeppelin
    [:release/name])

  )
