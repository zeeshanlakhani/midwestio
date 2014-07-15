(ns org.zeeshanlakhani.midwestio.core
  (:require [clojure.test.check :refer [quick-check]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [schema.core :as s]
            [schema-gen.core :refer [schema->gen]]
            [clojure.core.matrix :refer [transpose]]))

;; Don't Run - Example

(deftest test-post
  (with-system
    (let [config (load-config)
          port (:http-server-port config)
          chart-to-create
          {:slug "foo-bar-baz"
           :name "Foo Bar Baz"
           :description {:en "foo"}
           :duration-seconds 100
           :created-by {:url "/accounts/brianolssen"}
           :media [{:url "/tracks/baby-monkey"} {:url "/videos/oh-my-dayum"}]}
          req {:accept :json
               :throw-exceptions false
               :content-type "application/json"
               :basic-auth ["brianolssen" "iF41ixXSUntFoXirPAXJ"]
               :body (json/encode chart-to-create)}
          path (route port :charts)]
      (testing "Post chart";; ...))))

(def matrix-gen
  (gen/such-that
   not-empty
   (gen/vector
    (gen/tuple gen/int gen/int gen/int))))

(gen/sample matrix-gen 10)

(def matrix
  [[1 2]
   [3 4]])

(transpose matrix)

(def transpose-of-transpose-prop
  (prop/for-all [m matrix-gen]
  (= m (transpose (transpose m)))))

(quick-check 50 transpose-of-transpose-prop)

(defn reciprocal-sum [s]
  (reduce + (map (partial / 1) s)))

(reciprocal-sum '(1 2))

(def numbers
  (gen/fmap #(filter even? %)
   (gen/one-of [(gen/vector gen/int)
                (gen/list gen/int)])))

(gen/sample numbers)

(def reciprocal-sum-check
  (prop/for-all [s numbers]
    (number? (reciprocal-sum s))))

(quick-check 100 reciprocal-sum-check :max-size 300)

(def LookAtData
  {:foo                     s/Int
   :baz                     [s/Str]
   :bar                     s/Bool
   (s/optional-key :pirate) s/Keyword})

(s/validate LookAtData {:foo 1 :baz ["Hey"] :bar false})

(s/check LookAtData {:foo 1 :bar false :pirate :jj})

(def s-vector
  [(s/one s/Bool "first") (s/one {s/Keyword s/Bool} "second")
   (s/optional s/Keyword "maybe") s/Int])

(gen/sample (schema->gen s-vector) 10)

(def s-hashmap
  {:foo s/Int
   (s/optional-key :midwestio) s/Bool
   :baz [s/Str]
   :bar s-vector
   :bugz #"^[a-z0-9][a-z0-9\-]{0,40}$"
   s/Keyword s/Num})

(gen/sample (schema->gen s-hashmap) 10)

;; Don't Run - Example

(defmulti schema->gen  identity)
(defmulti schema->gen* class)

(defmethod schema->gen s/Bool
  [_]
  (gen/elements [true false]))

(defmethod schema->gen* schema.core.Maybe
  [e]
  (gen/one-of
   [(gen/return nil)
    (schema->gen (:schema e))]))

(defmethod schema->gen* ::gen-map
  [e]
  (let [required (for [[k v] e
                       :when (or (keyword? k)
                                (instance? schema.core.RequiredKey k))]
                   [k v])
        rest (apply dissoc e (map first required))
        [optional [repeated]] (split-with
                               (fn [[k v]]
                                 (instance? schema.core.OptionalKey k))
                               rest)]
    (g/apply-by
     (partial apply merge)
     (g/apply-by
      (partial into {})
     (map optional-key-gen optional))
     (if repeated
       (->> repeated (map schema->gen) (apply gen/map))
       (gen/return {}))
     (apply gen/hash-map
            (mapcat (fn [[k v]]
                      [k (schema->gen v)])
                    required)))))

;; Don't Run - Example

(defmacro test-put-gen
  {:requires [#'with-system config/load-config s/check http/get http/put
              #'valid! #'prop/for-all ugen/generate]}
  [{:keys [status resource-type ids schema resource-gen req-overrides]}]
  `(prop/for-all
    [gen# (ugen/generate ~schema ~resource-gen)]
    (with-system
      (let [cfg# (config/load-config)
            id# (rand-nth ~ids)
            path# (route (:http-server-port cfg#) ~resource-type id#)
            res# #(http/get path# common-req-get)
            source# #(get-body (res#))
            prop# (merge
                   (select-keys (source#) (:removes* ~resource-gen))
                   gen#)
            put-resp# (atom nil)
            continue# (atom true)]
        ;; Create our Generated Data & Validate
        (testing (str "Validate Generated " (name ~resource-type))
          (if-not (is (~'validates? ~schema prop#))
            (valid! ~schema prop#)
            (swap! continue# (constantly false))))
        ;; Make a PUT Request w/ this Data
        (when @continue#
          (testing "Test Put w/ Sampled Data, Check Status"
            (let [res# (http/put
                        path#
                        (put-post-body
                         (merge common-req-put ~req-overrides) prop#))]
              (if (is (= 200 (:status res#)))
                (swap! put-resp# (constantly (get-body res#)))
                (swap! continue# (constantly false))))))
        (when @continue#
          (testing "Validate Put Response"
            (when (is (~'validates? ~schema @put-resp#))
              (swap! continue# (constantly false)))))
        (when @continue#
          (testing "New Get Response Matches Previous Put Response"
            (let [res*# (res#)
                  get-resp# (get-body res*#)]
              (when (is (= 200 (:status res*#)))
                (is (= @put-resp# get-resp#))))))))))

;; Don't Run - Example

;; What Current Schema Looks Like
(def Account-Existing
  (assoc Account-Base
    :primary-lang sc/Language
    :biography c/Localized-Map
    :handle c/Handle
    :prior-handles [c/Handle]
    ;;:roles Account-Roles
    :created-at sc/ISO-Date-Time
    :images c/Images
    :homepage (s/maybe c/Simple-Link)
    :agency (s/maybe Account-Link)
    :agent (s/maybe Account-Link)
    :location (s/maybe Account-Location)))

(defn account-gen
  {:requires [s/check]}
  []
  (let [check-merge (:check-merge ugen/gen-ops)
        vector-merge (:vector-merge ugen/gen-ops)]
    {:biography ugen/localized-map
     :name ugen/non-empty-string
     :primary-lang ugen/language
     :removes* [:url :created-at :location :handle]
     :injections* {:agency (ugen/account-link-injection
                            check-merge :put)
                   :agent (ugen/account-link-injection
                           check-merge :put)
                   :homepage (ugen/url-only-injection
                              check-merge nil urls)
                   :images (ugen/url-only-injection
                            vector-merge nil images)}}))

;; Example only
;; Under the Hood
(defn generate
  "Generates a sampled set of data, combining schema->gen
  and custom generators.
  It also randomizes which fields, if optional types, may or
  may not be on a request."
  [schema gen-map]

  (comment
    "gen-map conforms to a hash-map with
    customized keyword-gens {:foo gen}, removals, and injections,
    e.g."

    {:biography non-empty-string
     :name non-empty-string
     :primary-lang language
     :removes [:field1 :field2]
     :injections* {:genmap
                   {:field3 #(gen/return "foo")}
                   :ops (:vector-merge gen-ops)}}

    "Injections allow for the ability to *inject* generators into
    previously generated data-responses. An injection needs the required
    keys *genmap* and *ops*; *genmap* values are functions.")

  (let [schema* (rename-keys schema (:non-optional* gen-map))
        samp (comp last doall gen/sample)
        opts (find-optional-keys schema*)
        gen-map-opts (rename-keys gen-map opts)
        removes-ks (:removes* (random-update-removals gen-map (keys opts)))
        removes-ks-opts (map #(s/optional-key %) removes-ks)
        m (walk/postwalk
           #(if (map? %)
              (-> (apply dissoc % (apply conj
                                         removes-ks
                                         removes-ks-opts))
                  (c/update-in-if-exists
                   (keys gen-map-opts)
                   ;; set default values for what will be custom gens
                   ;; java - number placeholder
                   (fn [_ _] java.lang.Number)))
              %) schema*)
        injections (apply dissoc (:injections* gen-map) removes-ks)
        ;; generation on basic, non-custom types
        init-gen (schema->gen m)]
    (gen/fmap (fn [m]
                (walk/postwalk
                 #(if (map? %)
                    (-> (c/update-in-if-exists
                         %
                         (keys gen-map)
                         (fn [_ k] (samp (k gen-map))))
                        (c/update-in-if-exists
                         (keys injections)
                         (fn [v k]
                           (when v (-> v
                                       (inject-gen
                                        (get-in injections [k :genmap])
                                        (get-in injections [k :ops]))
                                       samp))))) %) m)) init-gen)))
