export type Role = 'PLATFORM_ADMIN' | 'HOSPITAL_ADMIN' | 'DOCTOR' | 'NURSE';
export type CareSetting = 'INPATIENT' | 'OUTPATIENT';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type Formulation = 'TABLET' | 'CAPSULE' | 'SYRUP' | 'INJECTION' | 'INHALER' | 'DROPS' | 'OTHER';
export type DoseUnit = 'MG' | 'MCG' | 'G' | 'ML' | 'IU' | 'UNITS';
export type SlotStatus = 'PENDING' | 'HIT' | 'DELAYED' | 'MISSED' | 'REFUSED' | 'CANCELLED';
export type RegimenStatus = 'ACTIVE' | 'COMPLETED' | 'DISCONTINUED';

export const GENDERS: Gender[] = ['MALE', 'FEMALE', 'OTHER'];
export const CARE_SETTINGS: CareSetting[] = ['INPATIENT', 'OUTPATIENT'];
export const FORMULATIONS: Formulation[] = ['TABLET', 'CAPSULE', 'SYRUP', 'INJECTION', 'INHALER', 'DROPS', 'OTHER'];
export const DOSE_UNITS: DoseUnit[] = ['MG', 'MCG', 'G', 'ML', 'IU', 'UNITS'];

export interface AuthState {
  token: string;
  role: Role;
  hospitalId: string | null;
  fullName: string;
}

export interface Hospital { id: string; code: string; name: string; status: string }
export interface CreateHospitalInput {
  code: string; name: string; adminEmail: string; adminFullName: string; adminPassword: string;
}
export interface CreateHospitalResult {
  hospitalId: string; code: string; name: string; apiKey: string; adminStaffId: string;
}

export interface Staff { id: string; email: string; fullName: string; role: Role; specialty: string | null; active: boolean }
export interface StaffInput { email: string; fullName: string; password: string; role: Role; specialty: string | null }

export interface Patient {
  id: string; fullName: string; phone: string | null; guardianPhone: string | null;
  dateOfBirth: string; gender: Gender; careSetting: CareSetting; conditionCode: string;
  active: boolean; weightKg: number | null;
}
export interface PatientInput {
  fullName: string; phone: string | null; guardianPhone: string | null; dateOfBirth: string;
  gender: Gender; careSetting: CareSetting; conditionCode: string; weightKg: number | null;
}

export interface Regimen {
  id: string; patientId: string; prescribedBy: string; drugName: string; formulation: Formulation;
  doseAmount: number; doseUnit: DoseUnit; doseTimes: string[]; dosesPerDay: number;
  startDate: string; endDate: string | null; graceWindowMinutes: number;
  status: RegimenStatus; discontinuedOn: string | null;
}
export interface RegimenInput {
  drugName: string; formulation: Formulation; doseAmount: number; doseUnit: DoseUnit;
  doseTimes: string[]; startDate: string; endDate: string | null; graceWindowMinutes: number;
}
export interface RegimenUpdate {
  doseAmount?: number; doseUnit?: DoseUnit; doseTimes?: string[];
  endDate?: string; graceWindowMinutes?: number; reason?: string;
}
export interface RegimenChange {
  changeId: string; changedAt: string; changedByName: string | null; changedByRole: string;
  field: string; oldValue: string | null; newValue: string | null; reason: string | null;
}

export interface DoseSlot {
  id: string; patientId: string; patientName: string; drugName: string; formulation: Formulation;
  doseAmount: number; doseUnit: DoseUnit; careSetting: CareSetting; dueAt: string;
  windowClosesAt: string; status: SlotStatus; respondedAt: string | null;
}

export interface NurseAlert {
  id: string; doseSlotId: string; patientId: string; regimenId: string;
  dueAt: string; windowClosesAt: string; status: 'OPEN' | 'RESOLVED'; resolvedStatus: string | null;
}

export interface DrugRule {
  id: string; drugName: string; formulation: Formulation; minAgeYears: number; maxAgeYears: number;
  gender: Gender | null; minWeightKg: number | null; maxWeightKg: number | null;
  doseAmount: number; doseUnit: DoseUnit; strengthPerUnit: number | null; doseTimes: string[]; active: boolean;
}
export interface DrugRuleInput {
  drugName: string; formulation: Formulation; minAgeYears: number; maxAgeYears: number;
  gender: Gender | null; minWeightKg: number | null; maxWeightKg: number | null;
  doseAmount: number; doseUnit: DoseUnit; strengthPerUnit: number | null; doseTimes: string[];
}
export interface Suggestion {
  ruleId: string; formulation: Formulation; doseAmount: number; doseUnit: DoseUnit;
  doseTimes: string[]; unitsToAdminister: number | null; unitsLabel: string | null;
}
export interface SuggestionResponse {
  patientId: string; drugName: string; ageYears: number; weightKg: number | null;
  suggestions: Suggestion[]; notes: string[];
}

export interface SideEffectReport {
  id: string; patientId: string; regimenId: string; sideEffect: string;
  reportedByName: string | null; reportedByRole: string; reportedAt: string;
  status: 'ACTIVE' | 'RETRACTED'; retractedAt: string | null; retractReason: string | null;
}

export interface AnalyticsResult<T> { rows: T[]; minGroupSize: number; includesSynthetic: boolean }
export interface DayPoint { day: number; doses: number; patients: number; takenRate: number | null; onTimeRate: number | null; suppressed: boolean }
export interface DrugRow { drugName: string; doses: number; patients: number; takenRate: number | null; onTimeRate: number | null; suppressed: boolean }
export interface SideEffectRow {
  ageBucket: string; gender: string; sideEffect: string; patientsReporting: number;
  cohortPatients: number; ratePercent: number | null; suppressed: boolean;
}
export interface HospitalRow {
  label: string; mine: boolean; doseAmount: number; doseUnit: string; doses: number; patients: number;
  takenRate: number | null; onTimeRate: number | null; suppressed: boolean;
}
export interface SummaryRow {
  careSetting: string; doses: number; patients: number; hit: number; delayed: number; missed: number;
  refused: number; takenRate: number | null; onTimeRate: number | null; suppressed: boolean;
}
