export type Role = 'USER' | 'MANAGER' | 'SERVICE_SPECIALIST'

export type AuthPayload = {
  accessToken: string
  refreshToken: string
  tokenType: string
  email: string
  role: Role
}

export type ServiceItem = {
  id: number
  title: string
  subtitle: string | null
  description: string
  category: string
  basePrice: number
  active: boolean
  coverImageUrl: string | null
  specs: { key: string; value: string }[]
  images: { imageUrl: string; altText: string | null; cover: boolean }[]
}

export type Paged<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type SavedService = {
  serviceId: number
  title: string
  category: string
  basePrice: number
  savedAt: string
}

export type ServiceRequest = {
  requestId: number
  serviceId: number
  serviceTitle: string
  equipmentId: number
  equipmentInventoryCode: string
  equipmentModelName: string
  userId: number
  userEmail: string
  serviceBasePrice: number
  rentalStartDate: string
  rentalEndDate: string
  rentalDays: number
  returnedAt: string | null
  notes: string
  status: ServiceRequestStatus
  createdAt: string
  specialistConclusionRecorded?: boolean
}

export type InvoiceStatus = 'ISSUED' | 'PAID' | 'CANCELLED'

export type Invoice = {
  id: number
  userId: number
  serviceRequestId: number | null
  defectReportId: number | null
  invoiceType: string
  amount: number
  currency: string
  status: InvoiceStatus | string
  description: string
  createdAt: string
  paidAt: string | null
  serviceTitle?: string | null
  rentalStartDate?: string | null
  rentalEndDate?: string | null
}

export type Profile = {
  email: string
  fullName: string
  phoneNumber: string | null
  virtualBalance: number
}

export type UserStatus = 'ACTIVE' | 'BLOCKED'
export type ServiceRequestStatus = 'NEW' | 'AWAITING_PAYMENT' | 'IN_PROGRESS' | 'AWAITING_SPECIALIST_REVIEW' | 'COMPLETED' | 'CANCELLED'
export type DefectReportStatus = 'NEW' | 'SENT_TO_MANAGER' | 'APPROVED' | 'REJECTED'
export type DefectSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type InvoiceType = 'SERVICE' | 'PENALTY'

export type AdminUser = {
  id: number
  email: string
  fullName: string
  status: UserStatus
  role: Role
  createdAt: string
}

export type ManagerServiceRequest = {
  requestId: number
  serviceId: number
  serviceTitle: string
  equipmentId: number
  equipmentInventoryCode: string
  equipmentModelName: string
  userId: number
  userEmail: string
  serviceBasePrice: number
  rentalStartDate: string
  rentalEndDate: string
  rentalDays: number
  returnedAt: string | null
  notes: string
  status: ServiceRequestStatus
  createdAt: string
  /** Заключение/ведомость специалиста передана или рассмотрена (можно закрыть заявку). */
  specialistConclusionRecorded?: boolean
}

export type DefectReport = {
  id: number
  equipmentId: number
  equipmentInventoryCode: string
  equipmentModelName: string
  specialistUserId: number
  serviceRequestId: number | null
  penaltyInvoiceId: number | null
  penaltyInvoiceStatus: string
  defectDescription: string
  severity: DefectSeverity
  recommendedPenalty: number
  /** Доп. накопительный износ %, применяемый при утверждении ведомости. */
  plannedExtraWearPercent: number
  status: DefectReportStatus
  createdAt: string
}

export type RevenueReport = {
  fromDate: string
  toDate: string
  paidInvoices: number
  totalRevenue: number
  serviceRevenue: number
  penaltyRevenue: number
}

export type AnalyticsReport = {
  fromDate: string
  toDate: string
  requestsCreated: number
  invoicesIssued: number
  invoicesPaid: number
  totalRevenue: number
  averagePaidCheck: number
  paymentConversionPercent: number
  daily: {
    date: string
    revenue: number
    requestsCreated: number
    paidInvoices: number
  }[]
  requestStatusBreakdown: Record<string, number>
  topServices: {
    serviceId: number
    serviceTitle: string
    requestsCount: number
    revenue: number
  }[]
}

export type EquipmentState = {
  historyId: number
  equipmentId: number
  conditionLabel: string
  calculatedWearPercent: number
  amortizationValue: number
  recordedAt: string
  criticalConditionDetected: boolean
}

export type EquipmentOption = {
  id: number
  inventoryCode: string
  modelName: string
  accumulatedWearPercent: number
  rentalWearRatePerDay: number
}

export type SpecialistEquipmentInspectionSnapshot = {
  historyId: number
  conditionLabel: string
  calculatedWearPercent: number
  amortizationValue: number
  recordedAt: string
}

export type SpecialistEquipmentOverview = {
  equipmentId: number
  inventoryCode: string
  modelName: string
  category: string
  catalogTitle: string
  lifecycleStatus: string
  accumulatedWearPercent: number
  rentalWearRatePerDay: number
  latestInspection: SpecialistEquipmentInspectionSnapshot | null
}

export type EquipmentStateEventType = 'INSPECTION' | 'RENTAL_WEAR' | 'DEFECT_WEAR'

export type EquipmentStateTimelineEntry = {
  eventType: EquipmentStateEventType
  occurredAt: string
  title: string
  description: string
  stateHistoryId: number | null
  wearLedgerId: number | null
  serviceRequestId: number | null
  defectReportId: number | null
  inspectionCalculatedWearPercent: number | null
  rentalWearDelta: number | null
  defectWearDelta: number | null
  rentalDays: number | null
  rentalRatePerDay: number | null
}

export type SpecialistServiceRequestOption = {
  requestId: number
  serviceId: number
  serviceTitle: string
  userEmail: string
  equipmentId: number
  equipmentInventoryCode: string
  equipmentModelName: string
  latestEquipmentState: {
    historyId: number
    conditionLabel: string
    calculatedWearPercent: number
    amortizationValue: number
    recordedAt: string
  } | null
}

export type EquipmentRepairAlert = {
  equipmentId: number
  inventoryCode: string
  modelName: string
  accumulatedWearPercent: number
  latestHistoryId: number | null
  latestConditionLabel: string | null
  latestWearPercent: number | null
  latestAmortizationValue: number | null
  latestRecordedAt: string | null
}

export type GeocodingEstimate = {
  inputAddress: string
  normalizedAddress: string
  latitude: number
  longitude: number
  distanceKm: number
  logisticsSurcharge: number
}
