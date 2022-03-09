import Foundation
import Capacitor
import StoreKit
import SwiftyStoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(InAppPurchasesPlugin)
public class InAppPurchasesPlugin: CAPPlugin {
    private let implementation = InAppPurchases()
    
    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.echo(value)
        ])
    }
    
    @objc func completeTransactions(_ call: CAPPluginCall) {
        let atomically = call.getBool("atomically") ?? false
        implementation.completeTransactions(atomically) { purchases in
            var purchasesArray: [PluginCallResultData] = []
            for purchase in purchases {
                purchasesArray.append([
                    "productId": purchase.productId,
                    "quantity": purchase.quantity,
                    "transaction": [
                        "transactionDate": (purchase.transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000,
                        "transactionState": purchase.transaction.transactionState.stringState,
                        "transactionIdentifier": purchase.transaction.transactionIdentifier as Any
                    ],
                    "paymentTransaction": [
                        "transactionDate": (purchase.transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000,
                        "transactionState": purchase.transaction.transactionState.stringState,
                        "transactionIdentifier": purchase.transaction.transactionIdentifier as Any
                    ],
                    "needsFinishTransaction": purchase.needsFinishTransaction
                ])
            }
            call.resolve([
                "purchases": purchasesArray
            ])
        }
    }
    
    @objc func purchaseProduct(_ call: CAPPluginCall) {
        let productId = call.getString("productId") ?? ""
        let quantity = call.getInt("quantity") ?? 1
        let atomically = call.getBool("atomically") ?? false
        let applicationUsername = call.getString("applicationUsername") ?? ""
        let simulatesAskToBuyInSandbox = call.getBool("simulatesAskToBuyInSandbox") ?? false
        implementation.purchaseProduct(productId: productId, quantity: quantity, atomically: atomically, applicationUsername: applicationUsername, simulatesAskToBuyInSandbox: simulatesAskToBuyInSandbox) { purchaseResult in
            
            switch purchaseResult {
            case .success(let product):
                call.resolve(
                    [
                        "productId": product.productId,
                        "quantity": product.quantity,
                        "transaction": [
                            "transactionDate": (product.transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000,
                            "transactionState": product.transaction.transactionState.stringState,
                            "transactionIdentifier": product.transaction.transactionIdentifier as Any
                        ],
                        "paymentTransaction": [
                            "transactionDate": (product.transaction.transactionDate?.timeIntervalSince1970 ?? 0) * 1000,
                            "transactionState": product.transaction.transactionState.stringState,
                            "transactionIdentifier": product.transaction.transactionIdentifier as Any
                        ],
                        "needsFinishTransaction": product.needsFinishTransaction
                    ]
                )
    
            case .error(let error):
                switch error.code {
                case .unknown: call.reject("Unknown error. Please contact support")
                case .clientInvalid: call.reject("Not allowed to make the payment")
                case .paymentCancelled: call.reject("Payment cancelled")
                case .paymentInvalid: call.reject("The purchase identifier was invalid")
                case .paymentNotAllowed: call.reject("The device is not allowed to make the payment")
                case .storeProductNotAvailable: call.reject("The product is not available in the current storefront")
                case .cloudServicePermissionDenied: call.reject("Access to cloud service information is not allowed")
                case .cloudServiceNetworkConnectionFailed: call.reject("Could not connect to the network")
                case .cloudServiceRevoked: call.reject("User has revoked permission to use this cloud service")
                default: call.reject((error as NSError).localizedDescription)
                }
            }
        }
    }
    
    @objc func finishTransaction(_ call: CAPPluginCall) {
        let transactionIdentifier = call.getString("transactionIdentifier") ?? ""
        do {
            try implementation.finishTransaction(transactionIdentifier)
            call.resolve()
        } catch let e as NSError {
            call.reject(e.userInfo["message"] as? String ?? "Unknown error")
        }
    }
    
    @objc func fetchReceipt(_ call: CAPPluginCall) {
        let forceRefresh = call.getBool("forceRefresh") ?? false
        implementation.fetchReceipt(forceRefresh: forceRefresh) { result in
            switch result {
            case .success(let receiptData):
                let encryptedReceipt = receiptData.base64EncodedString(options: [])
                print("Fetch receipt success:\n\(encryptedReceipt)")
                call.resolve(["receiptData": encryptedReceipt])
            case .error(let error):
                call.reject("Fetch receipt failed: \(error)")
            }
        }
    }
}

private extension SKPaymentTransactionState {
    
    var stringState: String {
        switch self {
        case .purchasing:
            return "purchasing"
        case .purchased:
            return "purchased"
        case .failed:
            return "failed"
        case .restored:
            return "restored"
        case .deferred:
            return "deferred"
        @unknown default:
            return "unknown"
        }
    }
}
