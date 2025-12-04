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
            "email" to (user.email ?: ""),
            // Initialize mylist, wantlist and selllist as empty lists
            "mylist" to emptyList<LegoSet>(),
            "wantlist" to emptyList<LegoSet>(),
            "selllist" to emptyList<LegoSet>(),
            // Initialize sales statistics
            "setsSold" to 0,
            "totalEarned" to 0.0
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

    fun getEmail(userUid: String, onComplete: (String?) -> Unit) {
        firestore.collection("users").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                val email = document.get("email") as? String
                onComplete(email)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting email for $userUid", e)
                onComplete(null)
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
                val cities = mutableListOf<String>()

                for (document in query.documents) {
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
     * Add set to user's mylist in firestore
     */
    fun addSetToMyList(userUid: String, set: LegoSet) {
        firestore.collection("users").document(userUid)
            .update("mylist", FieldValue.arrayUnion(set))
            .addOnSuccessListener { Log.d(TAG, "Mylist updated") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating mylist", e) }
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
     * Get sets from user's mylist from firestore
     */
    fun getSetsFromMyList(userUid: String, onComplete: (List<LegoSet>?) -> Unit){
        firestore.collection("users").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val myList = document.get("mylist") as? List<HashMap<String, Any>>
                    if (myList != null) {
                        val legoSets = myList.map { map ->
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
                Log.w(TAG, "Error getting mylist", e)
                onComplete(null)
            }
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

    fun getBuyList(currentUserUid: String, onComplete: (List<MarketSellEntry>) -> Unit){
        firestore.collection("users")
            .get()
            .addOnSuccessListener { query ->
                val result = mutableListOf<MarketSellEntry>()
                Log.d(TAG, "getBuyList: found ${query.size()} user documents")

                for(document in query.documents){
                    val sellerUid = document.get("uid") as? String ?: ""

                    // Skip the current user's items
                    if (sellerUid == currentUserUid) {
                        Log.d(TAG, "getBuyList: Skipping current user's items")
                        continue
                    }

                    val sellerCity = document.get("city") as? String
                    val sellList = document.get("selllist") as? List<HashMap<String, Any>>

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

                Log.d(TAG, "getBuyList: Returning ${result.size} items for purchase")
                onComplete(result)

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "getBuyList: FAILED to read sell lists", e)
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
     * Remove set from user's mylist in firestore
     */
    fun removeSetFromMyList(userUid: String, set: LegoSet) {
        firestore.collection("users").document(userUid)
            .update("mylist", FieldValue.arrayRemove(set))
            .addOnSuccessListener {
                Log.d(TAG, "Set removed from mylist")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error removing set from mylist", e)
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
     * Record a sale - increment setsSold and add to totalEarned
     */
    fun recordSale(userUid: String, salePrice: Double) {
        firestore.collection("users").document(userUid)
            .update(
                mapOf(
                    "setsSold" to FieldValue.increment(1),
                    "totalEarned" to FieldValue.increment(salePrice)
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Sale recorded: setsSold +1, totalEarned +$salePrice")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error recording sale", e)
            }
    }

    /**
     * Data class to hold user statistics
     */
    data class UserStats(
        val setsOwned: Int,
        val setsSold: Int,
        val totalEarned: Double
    )

    /**
     * Get user statistics (sets owned, sets sold, total earned)
     */
    fun getUserStats(userUid: String, onComplete: (UserStats?) -> Unit) {
        firestore.collection("users").document(userUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get mylist count for sets owned
                    val myList = document.get("mylist") as? List<*>
                    val setsOwned = myList?.size ?: 0

                    // Get sales statistics
                    val setsSold = (document.get("setsSold") as? Long)?.toInt() ?: 0
                    val totalEarned = document.get("totalEarned") as? Double ?: 0.0

                    val stats = UserStats(
                        setsOwned = setsOwned,
                        setsSold = setsSold,
                        totalEarned = totalEarned
                    )

                    Log.d(TAG, "Retrieved stats: $stats")
                    onComplete(stats)
                } else {
                    Log.w(TAG, "User document does not exist")
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user stats", e)
                onComplete(null)
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