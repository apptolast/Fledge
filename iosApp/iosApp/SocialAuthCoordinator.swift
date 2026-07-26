import AuthenticationServices
import CryptoKit
import UIKit
import Shared

final class SocialAuthCoordinator: NSObject {
    static let shared = SocialAuthCoordinator()

    private var appleCompletion: ((String?) -> Void)?
    private var currentNonce: String?
    private var currentController: ASAuthorizationController?

    func registerBridges() {
        IosAppleAuthBridge.shared.signInHandler = { [weak self] completion in
            self?.signInWithApple { payload in
                _ = completion(payload)
            }
        }
    }

    private func signInWithApple(completion: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            let nonce = Self.randomNonceString()
            self.currentNonce = nonce
            self.appleCompletion = completion

            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = Self.sha256(nonce)

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            self.currentController = controller
            controller.performRequests()
        }
    }

    private func finish(_ payload: String?) {
        let completion = appleCompletion
        appleCompletion = nil
        currentNonce = nil
        currentController = nil
        completion?(payload)
    }

    private static func randomNonceString(length: Int = 32) -> String {
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remaining = length
        while remaining > 0 {
            var randoms = [UInt8](repeating: 0, count: 16)
            let status = SecRandomCopyBytes(kSecRandomDefault, randoms.count, &randoms)
            if status != errSecSuccess {
                fatalError("Unable to generate nonce. SecRandomCopyBytes failed with \(status)")
            }
            for random in randoms {
                if remaining == 0 {
                    break
                }
                if random < charset.count {
                    result.append(charset[Int(random)])
                    remaining -= 1
                }
            }
        }
        return result
    }

    private static func sha256(_ input: String) -> String {
        let hashed = SHA256.hash(data: Data(input.utf8))
        return hashed.map {
            String(format: "%02x", $0)
        }.joined()
    }
}

extension SocialAuthCoordinator: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard
            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = credential.identityToken,
            let idToken = String(data: tokenData, encoding: .utf8),
            let rawNonce = currentNonce
        else {
            finish(nil)
            return
        }

        let name = [credential.fullName?.givenName, credential.fullName?.familyName]
            .compactMap { $0 }
            .joined(separator: " ")
        finish("\(idToken)|||\(rawNonce)|||\(name)")
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        finish(nil)
    }
}

extension SocialAuthCoordinator: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene
        return windowScene?.windows.first(where: { $0.isKeyWindow })
            ?? windowScene?.windows.first
            ?? ASPresentationAnchor()
    }
}
