//package trustrail.api.service.integration;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.stereotype.Service;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//@FeignClient(
//        name = "pwaClient",
//        url = "${pwa.base-url}"
//)
//
//@Service
//public interface PWAClient {
//
//    @PostMapping
//    PWACreateMerchantResponse sendTransaction(
//            @RequestBody PWACreateMerchantRequest request
//    );
//}
//
