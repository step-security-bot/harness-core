BEGIN;
CREATE TABLE IF NOT EXISTS DEPLOYMENT_STEP (
    ID TEXT NOT NULL,
    ACCOUNT_ID TEXT NOT NULL,
    APP_ID TEXT NOT NULL,
    STEP_NAME TEXT NOT NULL,
    STEP_TYPE TEXT NOT NULL,
    STATUS VARCHAR(20),
    FAILURE_DETAILS TEXT,
    START_TIME BIGINT,
    END_TIME BIGINT,
    DURATION BIGINT,
    PARENT_TYPE TEXT,
    EXECUTION_ID TEXT NOT NULL,
    APPROVED_BY TEXT,
    APPROVAL_TYPE TEXT,
    APPROVED_AT BIGINT,
    APPROVAL_COMMENT TEXT,
    APPROVAL_EXPIRY BIGINT,
    PRIMARY KEY(ID)
);
COMMIT;

SELECT CREATE_HYPERTABLE('DEPLOYMENT_STEP','START_TIME',if_not_exists => TRUE);
