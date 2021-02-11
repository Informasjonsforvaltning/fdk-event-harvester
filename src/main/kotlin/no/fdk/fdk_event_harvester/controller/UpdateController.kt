package no.fdk.fdk_event_harvester.controller

import no.fdk.fdk_event_harvester.service.EndpointPermissions
import no.fdk.fdk_event_harvester.service.UpdateService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/update")
class UpdateController(
    private val endpointPermissions: EndpointPermissions,
    private val updateService: UpdateService) {

    @PostMapping("/meta")
    fun updateMetaData(@RequestHeader("X-API-KEY") apiKey: String?): ResponseEntity<Void> =
        if (endpointPermissions.isFromFDKCluster(apiKey)) {
            updateService.updateMetaData()
            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }

}
