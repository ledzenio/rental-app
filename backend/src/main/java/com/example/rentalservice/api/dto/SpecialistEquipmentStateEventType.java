package com.example.rentalservice.api.dto;

public enum SpecialistEquipmentStateEventType {
    /** Запись осмотра / телеметрии специалистом */
    INSPECTION,
    /** Начисление износа по завершённой аренде */
    RENTAL_WEAR,
    /** Начисление износа по утверждённой дефектной ведомости */
    DEFECT_WEAR
}
