package org.memmcol.gridflexbackendservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.service.band.BandService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;


@RestController
@RequestMapping("/band/service")
@Tag(name = "Band", description = "Band Management APIs")
public class BandController {

    @Autowired
    private BandService service;

    @Autowired
    private GlobalExceptionHandler exception;

    /**
     * @api {post} /band/service/create Create Band
     * @apiName CreateBand
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Creates a new band record.
     *
     * @apiBody {String} [id] Internal unique identifier of the record
     * @apiBody {String} [orgId] Organization identifier
     * @apiBody {String} [bandId] Band identifier
     * @apiBody {String} name Name of the band
     * @apiBody {String} [hour] Hour or time configuration for the band
     * @apiBody {String} [createdBy] User identifier of the creator
     * @apiBody {String} [approveBy] User identifier of the approver
     * @apiBody {String} [approveStatus] Approval status of the band (e.g. PENDING, APPROVED, REJECTED)
     * @apiBody {Object} [oldBandInfo] Previous band information object
     * @apiBody {String} [description] Band description
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object} data Created band details
     * @apiSuccess {String} data.id Internal unique identifier
     * @apiSuccess {String} data.orgId Organization identifier
     * @apiSuccess {String} data.bandId Band identifier
     * @apiSuccess {String} data.name Band name
     * @apiSuccess {String} data.hour Hour or time configuration
     * @apiSuccess {String} data.createdBy Creator identifier
     * @apiSuccess {String} data.approveBy Approver identifier
     * @apiSuccess {String} data.approveStatus Approval status
     * @apiSuccess {Object} data.oldBandInfo Previous band information
     * @apiSuccess {String} data.description Band description
     *
     * @apiError (400) BadRequest Invalid request payload
     * @apiError (500) InternalServerError Server error while creating band
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBand(@RequestBody Band band) {
        try {
            Map<String, Object> result = service.createBand(band);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }

    }

    /**
     * @api {put} /band/service/update Update Band
     * @apiName UpdateBand
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Updates an existing band record.
     *
     * @apiBody {String} id Internal unique identifier of the record
     * @apiBody {String} [orgId] Organization identifier
     * @apiBody {String} [bandId] Band identifier
     * @apiBody {String} [name] Name of the band
     * @apiBody {String} [hour] Hour or time configuration for the band
     * @apiBody {String} [createdBy] User identifier of the creator
     * @apiBody {String} [approveBy] User identifier of the approver
     * @apiBody {String} [approveStatus] Approval status of the band (e.g. PENDING, APPROVED, REJECTED)
     * @apiBody {Object} [oldBandInfo] Previous band information object
     * @apiBody {String} [description] Band description
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object} data Updated band details
     * @apiSuccess {String} data.id Internal unique identifier
     * @apiSuccess {String} data.orgId Organization identifier
     * @apiSuccess {String} data.bandId Band identifier
     * @apiSuccess {String} data.name Band name
     * @apiSuccess {String} data.hour Hour or time configuration
     * @apiSuccess {String} data.createdBy Creator identifier
     * @apiSuccess {String} data.approveBy Approver identifier
     * @apiSuccess {String} data.approveStatus Approval status
     * @apiSuccess {Object} data.oldBandInfo Previous band information
     * @apiSuccess {String} data.description Band description
     *
     * @apiError (400) BadRequest Invalid request payload
     * @apiError (404) NotFound Band not found
     * @apiError (500) InternalServerError Server error while updating band
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateBand(@RequestBody Band band) {
        try {
            Map<String, Object> result = service.updateBand(band);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    /**
     * @api {get} /band/service/all Get All Bands
     * @apiName GetAllBands
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Retrieves all bands with optional filtering, searching, and sorting.
     *
     * @apiParam {String} [type] Optional filter value
     * @apiParam {String} [search] Optional search keyword
     * @apiParam {String} [sort] Optional sort expression
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object[]} data List of band records
     * @apiSuccess {String} data.id Internal unique identifier
     * @apiSuccess {String} data.orgId Organization identifier
     * @apiSuccess {String} data.bandId Band identifier
     * @apiSuccess {String} data.name Band name
     * @apiSuccess {String} data.hour Hour or time configuration
     * @apiSuccess {String} data.createdBy Creator identifier
     * @apiSuccess {String} data.approveBy Approver identifier
     * @apiSuccess {String} data.approveStatus Approval status
     * @apiSuccess {Object} data.oldBandInfo Previous band information
     * @apiSuccess {String} data.description Band description
     *
     * @apiError (400) BadRequest Invalid request parameters
     * @apiError (500) InternalServerError Server error while retrieving bands
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllBands(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "sort", required = false, defaultValue = "") String sort
    ) {
        try {
            Map<String, Object> result = service.getBands(type, search, sort);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    /**
     * @api {get} /band/service/single Get Single Band
     * @apiName GetSingleBand
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Retrieves a single band using either bandId or bandVersionId.
     *
     * @apiParam {String} [bandId] Band identifier
     * @apiParam {String} [bandVersionId] Band version identifier
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object} data Band details
     * @apiSuccess {String} data.id Internal unique identifier
     * @apiSuccess {String} data.orgId Organization identifier
     * @apiSuccess {String} data.name Band name
     * @apiSuccess {String} data.hour Hour or time configuration
     * @apiSuccess {String} data.createdBy Creator identifier
     * @apiSuccess {String} data.approveBy Approver identifier
     * @apiSuccess {String} data.approveStatus Approval status
     * @apiSuccess {Object} data.oldBandInfo Previous band information
     * @apiSuccess {String} data.description Band description
     *
     * @apiError (400) BadRequest Missing or invalid request parameters
     * @apiError (404) NotFound Band not found
     * @apiError (500) InternalServerError Server error while retrieving band
     */
    @GetMapping("/single")
    public ResponseEntity<?> getSingleBand(
            @RequestParam(value = "bandId", required = false) UUID bandId,
            @RequestParam(value = "bandVersionId", required = false) UUID bandVersionId) {
        try {
            Map<String, Object> result = service.getBand(bandId, bandVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    /**
     * @api {put} /band/service/approve Approve Band
     * @apiName ApproveBand
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Updates the approval status of a band.
     *
     * @apiParam {String} bandId Band identifier
     * @apiParam {String} approveStatus Approval status of the band (e.g. APPROVED, REJECTED, PENDING)
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object} data Approval result details
     * @apiSuccess {String} data.bandId Band identifier
     * @apiSuccess {String} data.approveStatus Updated approval status
     *
     * @apiError (400) BadRequest Missing or invalid request parameters
     * @apiError (404) NotFound Band not found
     * @apiError (500) InternalServerError Server error while approving band
     */
    @PutMapping("/approve")
    public ResponseEntity<?> approve(
            @RequestParam UUID bandId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.approve(bandId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @api {put} /band/service/bulk-approve Bulk Approve Bands
     * @apiName BulkApproveBands
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Approves multiple bands in a single request.
     *
     * @apiBody {Object[]} band List of band objects to approve
     * @apiBody {String} [band.name] Band name
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object[]} data Result of approved bands
     *
     * @apiError (400) BadRequest Invalid request payload
     * @apiError (500) InternalServerError Server error while bulk approving bands
     */
    @PutMapping("/bulk-approve")
    public ResponseEntity<?> bulkApprove(
            @RequestBody List<Band> band) {
        try {
            Map<String, Object> result = service.bulkApprove(band);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    /**
     * @api {patch} /band/service/change-state Change Band State
     * @apiName ChangeBandState
     * @apiGroup Band
     * @apiVersion 1.0.0
     *
     * @apiDescription Changes the active/inactive state of a band.
     *
     * @apiParam {String} bandId Band identifier
     * @apiParam {Boolean} status New state of the band (true = active, false = inactive)
     *
     * @apiSuccess {Boolean} status Indicates whether the request was successful
     * @apiSuccess {String} message Response message
     * @apiSuccess {Object} data Updated band state details
     * @apiSuccess {String} data.bandId Band identifier
     * @apiSuccess {Boolean} data.status Updated state of the band
     *
     * @apiError (400) BadRequest Missing or invalid request parameters
     * @apiError (404) NotFound Band not found
     * @apiError (500) InternalServerError Server error while changing band state
     */
    @PatchMapping("/change-state")
    public ResponseEntity<?> changeState(
            @RequestParam UUID bandId,
            @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.changeStatus(bandId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            Map<String, Object> result = service.clearCache();
            return ResponseEntity.ok(result);

        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
