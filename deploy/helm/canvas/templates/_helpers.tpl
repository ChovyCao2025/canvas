{{- define "canvas.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "canvas.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else if contains (include "canvas.name" .) .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "canvas.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "canvas.labels" -}}
app.kubernetes.io/name: {{ include "canvas.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.partOf }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "canvas.backendName" -}}
{{- $fullname := include "canvas.fullname" . -}}
{{- if hasPrefix (printf "%s-" $fullname) .Values.backend.name -}}
{{- .Values.backend.name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" $fullname .Values.backend.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "canvas.frontendName" -}}
{{- $fullname := include "canvas.fullname" . -}}
{{- if hasPrefix (printf "%s-" $fullname) .Values.frontend.name -}}
{{- .Values.frontend.name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" $fullname .Values.frontend.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
