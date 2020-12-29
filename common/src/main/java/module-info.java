/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.hl7.tinkar.service.CachingService;

@SuppressWarnings("module") // 7 in HL7 is not a version reference
module org.hl7.tinkar.common {
    requires java.base;
    requires org.eclipse.collections.api;
    exports org.hl7.tinkar.service;
    exports org.hl7.tinkar.util;
    uses CachingService;
}
