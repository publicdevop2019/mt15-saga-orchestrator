package com.hw.aggregate.tx;

import com.hw.aggregate.tx.representation.AdminBizTxCardRep;
import com.hw.shared.sql.SumPagedRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.hw.shared.AppConstant.*;

@RestController
@RequestMapping(produces = "application/json", path = "tasks")
public class BizTxController {
    @Autowired
    AdminBizTxApplicationService adminBizTaskApplicationService;


    @GetMapping("admin")
    public ResponseEntity<SumPagedRep<AdminBizTxCardRep>> readForAdminByQuery(@RequestParam(value = HTTP_PARAM_QUERY, required = false) String queryParam,
                                                                              @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
                                                                              @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String config) {
        return ResponseEntity.ok(adminBizTaskApplicationService.readByQuery(queryParam, pageParam, config));
    }

}
