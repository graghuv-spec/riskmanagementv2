package com.riskmanagement.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Risk risk = new Risk();
    private Seed seed = new Seed();

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    @PostConstruct
    public void validate() {
        if (risk.getPdDivisor() <= 0) {
            throw new IllegalStateException("app.risk.pd-divisor must be > 0");
        }
        if (risk.getMaxScore() <= 0) {
            throw new IllegalStateException("app.risk.max-score must be > 0");
        }
        if (risk.getMinScore() > risk.getMaxScore()) {
            throw new IllegalStateException("app.risk.min-score cannot be greater than app.risk.max-score");
        }
        if (seed.getAuditLookbackDays() < 1) {
            throw new IllegalStateException("app.seed.audit-lookback-days must be >= 1");
        }
        if (seed.getExpectedUserCount() < 0) {
            throw new IllegalStateException("app.seed.expected-user-count must be >= 0");
        }
        if (seed.isEnabled()) {
            if (!StringUtils.hasText(seed.getInstitutionName())) {
                throw new IllegalStateException("app.seed.institution-name must be set when seeding is enabled");
            }
            if (!StringUtils.hasText(seed.getInstitutionLicenseNumber())) {
                throw new IllegalStateException("app.seed.institution-license-number must be set when seeding is enabled");
            }
            if (!StringUtils.hasText(seed.getInstitutionContactEmail())) {
                throw new IllegalStateException("app.seed.institution-contact-email must be set when seeding is enabled");
            }
            if (!StringUtils.hasText(seed.getInstitutionSubscriptionPlan())) {
                throw new IllegalStateException("app.seed.institution-subscription-plan must be set when seeding is enabled");
            }
            if (!StringUtils.hasText(seed.getAdminUser().getName())
                    || !StringUtils.hasText(seed.getAdminUser().getEmail())
                    || !StringUtils.hasText(seed.getAdminUser().getRole())) {
                throw new IllegalStateException("app.seed.admin-user fields must be set when seeding is enabled");
            }
            if (!StringUtils.hasText(seed.getOfficerUser().getName())
                    || !StringUtils.hasText(seed.getOfficerUser().getEmail())
                    || !StringUtils.hasText(seed.getOfficerUser().getRole())) {
                throw new IllegalStateException("app.seed.officer-user fields must be set when seeding is enabled");
            }
            if (!StringUtils.hasText(seed.getDefaultUserPassword())) {
                throw new IllegalStateException("app.seed.default-user-password must be set when seeding is enabled");
            }
        }
    }

    public static class Cors {
        private String allowedOrigins = "*";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Risk {
        private double incomeStabilityWeight = 0.3;
        private double repaymentHistoryWeight = 0.3;
        private double collateralRatioWeight = 0.2;
        private double sectorRiskWeight = 0.1;
        private double locationRiskWeight = 0.1;

        private double minScore = 0.0;
        private double maxScore = 100.0;
        private double pdDivisor = 10.0;
        private double pdOffset = 5.0;
        private double recommendedLimitIncomeMultiplier = 0.5;

        private String modelVersion = "Rule-Based v1";
        private String explanationJson = "{\"method\": \"rule-based\"}";

        private double sectorTechnologyScore = 90.0;
        private double sectorFinanceScore = 80.0;
        private double sectorAgricultureScore = 60.0;
        private double sectorDefaultScore = 70.0;
        private double defaultRegionalScore = 75.0;

        private double gradeAThreshold = 80.0;
        private double gradeBThreshold = 60.0;
        private double gradeCThreshold = 40.0;

        public double getIncomeStabilityWeight() {
            return incomeStabilityWeight;
        }

        public void setIncomeStabilityWeight(double incomeStabilityWeight) {
            this.incomeStabilityWeight = incomeStabilityWeight;
        }

        public double getRepaymentHistoryWeight() {
            return repaymentHistoryWeight;
        }

        public void setRepaymentHistoryWeight(double repaymentHistoryWeight) {
            this.repaymentHistoryWeight = repaymentHistoryWeight;
        }

        public double getCollateralRatioWeight() {
            return collateralRatioWeight;
        }

        public void setCollateralRatioWeight(double collateralRatioWeight) {
            this.collateralRatioWeight = collateralRatioWeight;
        }

        public double getSectorRiskWeight() {
            return sectorRiskWeight;
        }

        public void setSectorRiskWeight(double sectorRiskWeight) {
            this.sectorRiskWeight = sectorRiskWeight;
        }

        public double getLocationRiskWeight() {
            return locationRiskWeight;
        }

        public void setLocationRiskWeight(double locationRiskWeight) {
            this.locationRiskWeight = locationRiskWeight;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }

        public double getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(double maxScore) {
            this.maxScore = maxScore;
        }

        public double getPdDivisor() {
            return pdDivisor;
        }

        public void setPdDivisor(double pdDivisor) {
            this.pdDivisor = pdDivisor;
        }

        public double getPdOffset() {
            return pdOffset;
        }

        public void setPdOffset(double pdOffset) {
            this.pdOffset = pdOffset;
        }

        public double getRecommendedLimitIncomeMultiplier() {
            return recommendedLimitIncomeMultiplier;
        }

        public void setRecommendedLimitIncomeMultiplier(double recommendedLimitIncomeMultiplier) {
            this.recommendedLimitIncomeMultiplier = recommendedLimitIncomeMultiplier;
        }

        public String getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }

        public String getExplanationJson() {
            return explanationJson;
        }

        public void setExplanationJson(String explanationJson) {
            this.explanationJson = explanationJson;
        }

        public double getSectorTechnologyScore() {
            return sectorTechnologyScore;
        }

        public void setSectorTechnologyScore(double sectorTechnologyScore) {
            this.sectorTechnologyScore = sectorTechnologyScore;
        }

        public double getSectorFinanceScore() {
            return sectorFinanceScore;
        }

        public void setSectorFinanceScore(double sectorFinanceScore) {
            this.sectorFinanceScore = sectorFinanceScore;
        }

        public double getSectorAgricultureScore() {
            return sectorAgricultureScore;
        }

        public void setSectorAgricultureScore(double sectorAgricultureScore) {
            this.sectorAgricultureScore = sectorAgricultureScore;
        }

        public double getSectorDefaultScore() {
            return sectorDefaultScore;
        }

        public void setSectorDefaultScore(double sectorDefaultScore) {
            this.sectorDefaultScore = sectorDefaultScore;
        }

        public double getDefaultRegionalScore() {
            return defaultRegionalScore;
        }

        public void setDefaultRegionalScore(double defaultRegionalScore) {
            this.defaultRegionalScore = defaultRegionalScore;
        }

        public double getGradeAThreshold() {
            return gradeAThreshold;
        }

        public void setGradeAThreshold(double gradeAThreshold) {
            this.gradeAThreshold = gradeAThreshold;
        }

        public double getGradeBThreshold() {
            return gradeBThreshold;
        }

        public void setGradeBThreshold(double gradeBThreshold) {
            this.gradeBThreshold = gradeBThreshold;
        }

        public double getGradeCThreshold() {
            return gradeCThreshold;
        }

        public void setGradeCThreshold(double gradeCThreshold) {
            this.gradeCThreshold = gradeCThreshold;
        }
    }

    public static class Seed {
        private boolean enabled = false;
        private boolean resetBeforeSeed = false;
        private long expectedUserCount = 0;

        private String institutionName = "";
        private String institutionLicenseNumber = "";
        private String institutionContactEmail = "";
        private String institutionSubscriptionPlan = "";

        private DemoUser adminUser = new DemoUser("", "", "", true);
        private DemoUser officerUser = new DemoUser("", "", "", false);

        private String defaultUserPassword = "";
        private String defaultAuditIp = "";
        private int auditLookbackDays = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isResetBeforeSeed() {
            return resetBeforeSeed;
        }

        public void setResetBeforeSeed(boolean resetBeforeSeed) {
            this.resetBeforeSeed = resetBeforeSeed;
        }

        public long getExpectedUserCount() {
            return expectedUserCount;
        }

        public void setExpectedUserCount(long expectedUserCount) {
            this.expectedUserCount = expectedUserCount;
        }

        public String getInstitutionName() {
            return institutionName;
        }

        public void setInstitutionName(String institutionName) {
            this.institutionName = institutionName;
        }

        public String getInstitutionLicenseNumber() {
            return institutionLicenseNumber;
        }

        public void setInstitutionLicenseNumber(String institutionLicenseNumber) {
            this.institutionLicenseNumber = institutionLicenseNumber;
        }

        public String getInstitutionContactEmail() {
            return institutionContactEmail;
        }

        public void setInstitutionContactEmail(String institutionContactEmail) {
            this.institutionContactEmail = institutionContactEmail;
        }

        public String getInstitutionSubscriptionPlan() {
            return institutionSubscriptionPlan;
        }

        public void setInstitutionSubscriptionPlan(String institutionSubscriptionPlan) {
            this.institutionSubscriptionPlan = institutionSubscriptionPlan;
        }

        public DemoUser getAdminUser() {
            return adminUser;
        }

        public void setAdminUser(DemoUser adminUser) {
            this.adminUser = adminUser;
        }

        public DemoUser getOfficerUser() {
            return officerUser;
        }

        public void setOfficerUser(DemoUser officerUser) {
            this.officerUser = officerUser;
        }

        public String getDefaultUserPassword() {
            return defaultUserPassword;
        }

        public void setDefaultUserPassword(String defaultUserPassword) {
            this.defaultUserPassword = defaultUserPassword;
        }

        public String getDefaultAuditIp() {
            return defaultAuditIp;
        }

        public void setDefaultAuditIp(String defaultAuditIp) {
            this.defaultAuditIp = defaultAuditIp;
        }

        public int getAuditLookbackDays() {
            return auditLookbackDays;
        }

        public void setAuditLookbackDays(int auditLookbackDays) {
            this.auditLookbackDays = auditLookbackDays;
        }

        public static class DemoUser {
            private String name;
            private String email;
            private String role;
            private boolean mfaEnabled;

            public DemoUser() {
            }

            public DemoUser(String name, String email, String role, boolean mfaEnabled) {
                this.name = name;
                this.email = email;
                this.role = role;
                this.mfaEnabled = mfaEnabled;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            public boolean isMfaEnabled() {
                return mfaEnabled;
            }

            public void setMfaEnabled(boolean mfaEnabled) {
                this.mfaEnabled = mfaEnabled;
            }
        }
    }
}
