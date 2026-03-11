{{- define "riskmanagement.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "riskmanagement.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "riskmanagement.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "riskmanagement.labels" -}}
app.kubernetes.io/name: {{ include "riskmanagement.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "riskmanagement.postgresName" -}}
{{ include "riskmanagement.fullname" . }}-postgres
{{- end -}}

{{- define "riskmanagement.backendName" -}}
{{ include "riskmanagement.fullname" . }}-backend
{{- end -}}

{{- define "riskmanagement.frontendName" -}}
{{ include "riskmanagement.fullname" . }}-frontend
{{- end -}}
