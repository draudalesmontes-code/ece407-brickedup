package com.cs407.brickcollector.models

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class UserFirestore {

    private val firestore = Firebase.firestore

    /**
     *  Create a data object for the new user
     */
    fun saveUserToFireStore(
        user: FirebaseUser,
        name: String,
        city: String?,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        val db = Firebase.firestore
        val usersCollection = db.collection("users")

        val userData = hashMapOf(
            "uid" to user.uid,
            "name" to name,
            "city" to city,
            // Initialize wantlist and selllist as empty lists
            "wantlist" to emptyList<LegoSet>(),
            "selllist" to emptyList<LegoSet>()
        )

        // Set the data in a document named after the user's UID
        usersCollection.document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e)
            }
    }

    /**
     * Update city of user
     */
    fun updateCity(userUid: String, city: String?) {
        firestore.collection("users").document(userUid)
            .update("city", city)
            .addOnSuccessListener { Log.d(TAG, "City updated") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating city", e)
            }
    }

    /**
     * Get city of user
     */
    fun getCity(userUid: String, onComplete: (String?) -> Unit) {
        firestore.collection("users").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                val city = document.get("city") as? String
                onComplete(city)
            }
            .addOnFailureListener {
                Log.w(TAG, "Error getting city")
                onComplete(null)
            }
    }
    fun getAllCities(onComplete: (List<String>) -> Unit) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { query ->
                // How many docs did we get?

                val cities = mutableListOf<String>()

                for (document in query.documents) {
                    val data = document.data
                    val cityAny = document.get("city")

                    if (cityAny is String && cityAny.isNotBlank()) {
                        cities.add(cityAny.trim())
                    }
                }

                onComplete(cities)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "getAllCities: FAILED to read 'users' collection", e)
                onComplete(emptyList())
            }
    }



    /**
     * Add set to user's wantlist in firestore
     */
    fun addSetToWantList(userUid: String, set: LegoSet) {
        firestore.collection("users").document(userUid)
            .update("wantlist", FieldValue.arrayUnion(set))
            .addOnSuccessListener { Log.d(TAG, "Wantlist updated") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating Wantlist", e) }
    }

    /**
     * Add set to user's selllist in firestore
     */
    fun addSetToSellList(userUid: String, set: LegoSet) {
        firestore.collection("users").document(userUid)
            .update("selllist", FieldValue.arrayUnion(set))
            .addOnSuccessListener { Log.d(TAG, "Selllist updated") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating Selllist", e) }
    }

    /**
     * Get sets from user's wantlist from firestore
     */
    fun getSetsFromWantList(userUid: String, onComplete: (List<LegoSet>?) -> Unit){
        firestore.collection("users").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val wantList = document.get("wantlist") as? List<HashMap<String, Any>>
                    if (wantList != null) {
                        val legoSets = wantList.map { map ->
                            LegoSet(
                                name = map["name"] as? String ?: "No Name",
                                setId = (map["setId"] as? Long)?.toInt() ?: -1,
                                price = map["price"] as? Double ?: 0.0,
                                imageUrl = map["imageUrl"] as? String ?: "No image"
                            )
                        }
                        onComplete(legoSets)
                    } else {
                        onComplete(emptyList())
                    }
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener {e ->
                Log.w(TAG, "Error getting wantlist", e)
                onComplete(null)
            }
    }
    data class MarketSellEntry(
        val set: LegoSet,
        val sellerUid: String,
        val sellerCity: String?
    )

    fun getBuyList(onComplete: (List<MarketSellEntry>) -> Unit){
        firestore.collection("users")
            .get()
            .addOnSuccessListener { query ->
                val result = mutableListOf<MarketSellEntry>()
                Log.d(TAG, "getBuyList: found ${query.size()} user documents")
                for(document in query.documents){
                    val sellerCity = document.get("city") as? String
                    val sellList = document.get("selllist") as? List<HashMap<String, Any>>
                    val sellerUid = document.get("uid") as? String ?: ""

                    if (sellList != null) {
                        for (map in sellList) {
                            val set = LegoSet(
                                name = map["name"] as? String ?: "No Name",
                                setId = (map["setId"] as? Long)?.toInt() ?: -1,
                                price = map["price"] as? Double ?: 0.0,

                                imageUrl = map["imageUrl"] as? String ?: "No image"
                            )
                            result.add(
                                MarketSellEntry(
                                    set = set,
                                    sellerUid = sellerUid,
                                    sellerCity = sellerCity
                                )
                            )
                        }
                    }
                }

                onComplete(result)

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "getAllSellSetsWithCity: FAILED to read sell lists", e)
                onComplete(emptyList())
            }
    }


    /**
     * Get sets from user's selllist from firestore
     */
    fun getSetsFromSellList(userUid: String, onComplete: (List<LegoSet>?) -> Unit){
        firestore.collection("users").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val sellList = document.get("selllist") as? List<HashMap<String, Any>>
                    if (sellList != null) {
                        val legoSets = sellList.map { map ->
                            LegoSet(
                                name = map["name"] as? String ?: "No Name",
                                setId = (map["setId"] as? Long)?.toInt() ?: -1,
                                price = map["price"] as? Double ?: 0.0,
                                imageUrl = map["imageUrl"] as? String ?: "No image"
                            )
                        }
                        onComplete(legoSets)
                    } else {
                        onComplete(emptyList())
                    }
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener {e ->
                Log.w(TAG, "Error getting selllist", e)
                onComplete(null)
            }
    }

    /**
     * Remove set from user's wantlist in firestore
     */
    fun removeSetFromWantList(userUid: String, set: LegoSet) {
        firestore.collection("users").document(userUid)
            .update("wantlist", FieldValue.arrayRemove(set))
            .addOnSuccessListener {
                Log.d(TAG, "Set removed from wantlist")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error removing set from wantlist", e)
            }
    }

    /**
     * Remove set from user's selllist in firestore
     */
    fun removeSetFromSellList(userUid: String, set: LegoSet) {
        firestore.collection("users").document(userUid)
            .update("selllist", FieldValue.arrayRemove(set))
            .addOnSuccessListener {
                Log.d(TAG, "Set removed from selllist")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error removing set from selllist", e)
            }
    }


    /**
     * Delete user from firestore
     */
    fun removeUser(userUid: String) {
        firestore.collection("users").document(userUid)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "user data deleted") }
            .addOnFailureListener { e -> Log.w(TAG, "User deletion failed", e) }
    }
}