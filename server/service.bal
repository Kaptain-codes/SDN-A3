import ballerina/http;
import ballerina/sql;
import ballerina/time;
import ballerinax/postgresql;

configurable string dbHost = "localhost";
configurable int dbPort = 5432;
configurable string dbName = "stemmate";
configurable string dbUser = "stemmate";
configurable string dbPassword = ?;
configurable int httpPort = 9090;

final postgresql:Client database = check new (
    host = dbHost,
    port = dbPort,
    user = dbUser,
    password = dbPassword,
    database = dbName);

listener http:Listener listener = new (httpPort);

service / on listener {
    resource function post plans(@http:Payload json payload) returns http:Response {
        json[]|error incoming = payload.plans.ensureType();
        if incoming is error || incoming.length() == 0 || incoming.length() > 20 {
            return response(400, "The plans array must contain between 1 and 20 plans.");
        }

        json[] accepted = [];
        foreach json plan in incoming {
            if !completePlan(plan) {
                return response(400, "Every plan must contain all five required plan elements.");
            }
            string planId = check stringValue(plan.planId);
            string activityId = check stringValue(plan.activityId);
            string title = check stringValue(plan.title);
            string targetGroup = check stringValue(plan.targetGroup);
            string updatedAt = check stringValue(plan.updatedAt);
            string payloadText = plan.toJsonString();

            sql:ParameterizedQuery upsert = `INSERT INTO session_plans
                (plan_id, activity_id, title, target_group, payload_json, received_at, updated_at)
                VALUES (${planId}, ${activityId}, ${title}, ${targetGroup},
                    ${payloadText}::jsonb, CURRENT_TIMESTAMP, ${updatedAt})
                ON CONFLICT (plan_id) DO UPDATE SET
                    activity_id = EXCLUDED.activity_id,
                    title = EXCLUDED.title,
                    target_group = EXCLUDED.target_group,
                    payload_json = EXCLUDED.payload_json,
                    updated_at = EXCLUDED.updated_at
                WHERE session_plans.updated_at < EXCLUDED.updated_at`;
            sql:ExecutionResult|error saved = database->execute(upsert);
            if saved is error {
                return response(503, "The plan could not be stored.");
            }
            accepted.push({planId: planId, receivedAt: time:utcToString(time:utcNow())});
        }
        return {statusCode: 200, body: {accepted: accepted}};
    }

    resource function get sync/health() returns http:Response {
        sql:ExecutionResult|error result = database->execute(`SELECT 1`);
        if result is error {
            return response(503, "PostgreSQL is unavailable.");
        }
        return {statusCode: 200, body: {status: "ok"}};
    }
}

function completePlan(json plan) returns boolean {
    json[]|error steps = plan.steps.ensureType();
    json[]|error timing = plan.timingMinutes.ensureType();
    json[]|error materials = plan.materials.ensureType();
    json[]|error safety = plan.safetyNotes.ensureType();
    json[]|error inclusion = plan.inclusionPrompts.ensureType();
    if steps is error || timing is error || materials is error
        || safety is error || inclusion is error {
        return false;
    }
    return steps.length() > 0 && steps.length() == timing.length()
        && materials.length() > 0 && safety.length() > 0 && inclusion.length() > 0;
}

function stringValue(json value) returns string|error {
    return value.ensureType();
}

function response(int statusCode, string message) returns http:Response {
    return {statusCode: statusCode, body: {message: message}};
}
