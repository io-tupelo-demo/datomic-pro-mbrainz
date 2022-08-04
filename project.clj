(defproject io-tupelo-demo/datomic-pro-local "0.1.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [com.datomic/datomic-pro "1.0.6397"]
                 [org.clojure/clojure "1.11.1"]
                 [prismatic/schema "1.3.5"]
                 [tupelo "22.07.25a"]
                 ]
  :resource-paths ["resources/"]

  :global-vars { *warn-on-reflection* false }

  :update :daily ;  :always

  :target-path "target/%s"
  :clean-targets [ "target" ]

  :jvm-opts ["-Xms500m" "-Xmx2g"]
)
