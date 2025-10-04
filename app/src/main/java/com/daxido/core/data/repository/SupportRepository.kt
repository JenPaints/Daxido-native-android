package com.daxido.core.data.repository

import com.daxido.user.presentation.support.ChatSession
import com.daxido.user.presentation.support.FAQ
import com.daxido.user.presentation.support.IssueType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun getFaqs(): List<FAQ> {
        return try {
            val snapshot = firestore.collection("faqs")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                FAQ(
                    id = doc.id,
                    question = doc.getString("question") ?: "",
                    answer = doc.getString("answer") ?: "",
                    category = doc.getString("category") ?: "general"
                )
            }
        } catch (e: Exception) {
            // Return default FAQs if Firebase is unavailable
            getDefaultFaqs()
        }
    }

    suspend fun initiateLiveChat(): ChatSession {
        val userId = getCurrentUserId()
        val chatData = hashMapOf(
            "userId" to userId,
            "status" to "waiting",
            "createdAt" to System.currentTimeMillis(),
            "platform" to "android"
        )

        val chatDoc = firestore.collection("livechats")
            .add(chatData)
            .await()

        return ChatSession(
            id = chatDoc.id,
            agentName = "Support Agent",
            startTime = System.currentTimeMillis()
        )
    }

    suspend fun getSupportPhoneNumber(): String {
        return try {
            val doc = firestore.collection("config")
                .document("support")
                .get()
                .await()

            doc.getString("phoneNumber") ?: "+91 1800 123 4567"
        } catch (e: Exception) {
            "+91 1800 123 4567"
        }
    }

    suspend fun getSupportEmailAddress(): String {
        return try {
            val doc = firestore.collection("config")
                .document("support")
                .get()
                .await()

            doc.getString("emailAddress") ?: "support@daxido.com"
        } catch (e: Exception) {
            "support@daxido.com"
        }
    }

    suspend fun reportIssue(
        issueType: IssueType,
        description: String,
        rideId: String? = null
    ): String {
        val userId = getCurrentUserId()
        val issueData = hashMapOf(
            "userId" to userId,
            "type" to issueType.name,
            "description" to description,
            "rideId" to rideId,
            "status" to "open",
            "priority" to getPriorityForIssueType(issueType),
            "createdAt" to System.currentTimeMillis(),
            "platform" to "android"
        )

        val doc = firestore.collection("support_tickets")
            .add(issueData)
            .await()

        return doc.id.takeLast(8).uppercase()
    }

    private fun getPriorityForIssueType(issueType: IssueType): String {
        return when (issueType) {
            IssueType.SAFETY_CONCERN -> "urgent"
            IssueType.PAYMENT_ISSUE -> "high"
            IssueType.DRIVER_COMPLAINT -> "high"
            IssueType.LOST_ITEM -> "medium"
            IssueType.TRIP_ISSUE -> "medium"
            IssueType.REFUND_REQUEST -> "medium"
            IssueType.APP_BUG -> "low"
            IssueType.OTHER -> "low"
        }
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    private fun getDefaultFaqs(): List<FAQ> {
        return listOf(
            FAQ(
                id = "1",
                question = "How do I book a ride?",
                answer = "Simply open the app, enter your pickup and destination locations, select your preferred vehicle type, and tap 'Book Ride'. You'll be matched with a nearby driver.",
                category = "booking"
            ),
            FAQ(
                id = "2",
                question = "How is the fare calculated?",
                answer = "Fare is calculated based on distance, time, vehicle type, and current demand. Base fare varies by vehicle type, and surge pricing may apply during peak hours.",
                category = "pricing"
            ),
            FAQ(
                id = "3",
                question = "Can I cancel my ride?",
                answer = "Yes, you can cancel your ride before the driver arrives. Cancellation fees may apply if you cancel after the driver has started towards your location.",
                category = "cancellation"
            ),
            FAQ(
                id = "4",
                question = "What payment methods are accepted?",
                answer = "We accept cash, credit/debit cards, UPI, and wallet payments. You can also add money to your Daxido wallet for seamless payments.",
                category = "payment"
            ),
            FAQ(
                id = "5",
                question = "How do I track my ride?",
                answer = "Once your ride is confirmed, you can track your driver's real-time location on the map. You'll also receive updates about the driver's arrival time.",
                category = "tracking"
            ),
            FAQ(
                id = "6",
                question = "What if I have a complaint?",
                answer = "You can report issues through the app's support section, call our 24/7 helpline, or email us at support@daxido.com. We take all complaints seriously.",
                category = "support"
            ),
            FAQ(
                id = "7",
                question = "Is it safe to ride with Daxido?",
                answer = "Yes, all our drivers are verified and background checked. We have safety features like SOS button, trip sharing, and 24/7 support to ensure your safety.",
                category = "safety"
            ),
            FAQ(
                id = "8",
                question = "How do I rate my driver?",
                answer = "After completing your ride, you'll be prompted to rate your driver and provide feedback. Your ratings help us maintain service quality.",
                category = "rating"
            )
        )
    }
}