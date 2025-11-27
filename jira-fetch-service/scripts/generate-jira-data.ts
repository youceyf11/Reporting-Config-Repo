import axios from "axios";
import { faker } from "@faker-js/faker";
import dotenv from "dotenv"; // Import dotenv

// Load environment variables from .env file
dotenv.config({ path: "../.env" });

// -------------------------
// 1. Jira Credentials
// -------------------------

// Retrieve from environment variables
const email = process.env.JIRA_USERNAME as string;
const apiToken = process.env.JIRA_API_TOKEN as string;
const JIRA_BASE_URL = process.env.JIRA_BASE_URL as string;

// Basic validation (optional but recommended)
if (!email || !apiToken || !JIRA_BASE_URL) {
    console.error("❌ ERROR: Missing one or more required environment variables (JIRA_USERNAME, JIRA_API_TOKEN, JIRA_BASE_URL). Please check your .env file or environment setup.");
    process.exit(1); // Exit with an error code
}

// Base64 encode email:apiToken
const authString = Buffer.from(`${email}:${apiToken}`).toString("base64");

const headers = {
    Authorization: `Basic ${authString}`,
    "Accept": "application/json",
    "Content-Type": "application/json",
};

// -------------------------
// 2. Configuration
// -------------------------

// Fixed project names/keys (you can still hardcode these or move to .env for more flexibility)
const FIXED_PROJECT_NAMES = ["Auto Project A", "Auto Project B", "Auto Project C"];
const FIXED_PROJECT_KEYS = ["PROJA", "PROJB", "PROJC"];

const projectCount = FIXED_PROJECT_NAMES.length;
const issuesPerProject = 40;

const issueTypes = ["Task", "Story", "Bug"];
const priorities = ["High", "Medium", "Low"];

// -------------------------
// 3. Helper Functions
// -------------------------

function randomDateInNovember2025() {
    const start = new Date(2025, 10, 1); // Nov 1, 2025
    const end = new Date(2025, 10, 24); // Up to Nov 24
    return faker.date.between({ from: start, to: end }).toISOString().slice(0, -1) + '+0000'; // Jira format
}

// -------------------------
// 4. Fetch Account ID
// -------------------------

async function getMyAccountId() {
    try {
        const response = await axios.get(`${JIRA_BASE_URL}/rest/api/3/myself`, { headers });
        console.log(`✔ Fetched account ID: ${response.data.accountId}`);
        return response.data.accountId;
    } catch (e: any) {
        console.error("❌ Failed to fetch account ID:", e.response?.data || e.message);
        throw e;
    }
}

// -------------------------
// 5. Create Jira Project
// -------------------------

async function createProject(key: string, name: string, leadAccountId: string) {
    try {
        const response = await axios.post(`${JIRA_BASE_URL}/rest/api/3/project`, {
            key,
            name,
            projectTypeKey: "software",
            projectTemplateKey: "com.pyxis.greenhopper.jira:gh-simplified-scrum-classic",
            leadAccountId,  // Explicitly set to your account ID
        }, { headers });

        console.log(`✔ Project created: ${response.data.key} (${name})`);
        return response.data.key; // Return the project key for issue creation

    } catch (e: any) {
        console.error("❌ Project creation error:", e.response?.data || e.message);
        throw e; // Rethrow to stop if project fails
    }
}

// -------------------------
// NEW: 5.5. Delete Jira Project (if exists)
// -------------------------
async function deleteProjectIfExists(projectKey: string) {
    try {
        await axios.delete(`${JIRA_BASE_URL}/rest/api/3/project/${projectKey}`, { headers });
        console.log(`🗑️ Existing project ${projectKey} deleted.`);
        return true;
    } catch (e: any) {
        if (e.response && e.response.status === 404) {
            console.log(`🔍 Project ${projectKey} not found, proceeding with creation.`);
            return false;
        }
        console.warn(`⚠️ Could not delete project ${projectKey}. It might not exist or there's a permission issue:`, e.response?.data || e.message);
        return false;
    }
}

// -------------------------
// 6. Create Issue with Original Estimate
// -------------------------

async function createIssue(projectKey: string) {
    const type = faker.helpers.arrayElement(issueTypes);
    const descriptionText = faker.lorem.paragraph(); // Generate plain text
    const originalEstimateSeconds = faker.number.int({ min: 3600, max: 14400 }); // 1-4 hours

    const payload: any = {
        fields: {
            project: { key: projectKey },
            summary: faker.hacker.phrase(),
            description: {  // Convert to ADF
                type: "doc",
                version: 1,
                content: [
                    {
                        type: "paragraph",
                        content: [
                            {
                                text: descriptionText,
                                type: "text"
                            }
                        ]
                    }
                ]
            },
            issuetype: { name: type },
            priority: { name: faker.helpers.arrayElement(priorities) },
            customfield_10016: faker.number.int({ min: 1, max: 13 }), // story points
            timetracking: { originalEstimateSeconds }, // Set estimate
        }
    };

    try {
        const response = await axios.post(
            `${JIRA_BASE_URL}/rest/api/3/issue`,
            payload,
            { headers }
        );

        console.log(`   → Issue created: ${response.data.key} with estimate ${originalEstimateSeconds / 3600}h`);
        // Return issue key and original estimate for later use
        return { issueKey: response.data.key, originalEstimateSeconds };

    } catch (e: any) {
        console.error("❌ Issue error:", e.response?.data || e.message);
        return null;
    }
}

// -------------------------
// 7. Add Worklog to Issue
// -------------------------

async function addWorklog(issueKey: string, originalEstimateSeconds: number) {
    const timeSpentSeconds = Math.round(originalEstimateSeconds * faker.number.float({ min: 0.8, max: 1.2 })); // 80-120% of estimate
    const started = randomDateInNovember2025();

    const payload = {
        timeSpentSeconds,
        started,
        comment: {
            type: "doc",
            version: 1,
            content: [
                {
                    type: "paragraph",
                    content: [
                        { type: "text", text: faker.lorem.sentence() }
                    ]
                }
            ]
        }
    };

    try {
        const response = await axios.post(
            `${JIRA_BASE_URL}/rest/api/3/issue/${issueKey}/worklog`,
            payload,
            { headers }
        );

        console.log(`     ✓ Worklog added to ${issueKey}: ${timeSpentSeconds / 3600}h at ${started}`);

    } catch (e: any) {
        console.error(`❌ Worklog error for ${issueKey}:`, e.response?.data || e.message);
    }
}

// -------------------------
// 8. Transition Issue to Done
// -------------------------

async function transitionToDone(issueKey: string) {
    try {
        // Get available transitions
        const transResponse = await axios.get(
            `${JIRA_BASE_URL}/rest/api/3/issue/${issueKey}/transitions`,
            { headers }
        );

        // Find a transition that leads to a status in the 'Done' category
        const doneTransition = transResponse.data.transitions.find((t: any) =>
            t.to.statusCategory.key === 'done' || // Most reliable for "Done" status category
            t.name.toLowerCase() === 'done' ||
            t.name.toLowerCase() === 'résolu' || // French
            t.name.toLowerCase() === 'resolve' || // Common alternative
            t.name.toLowerCase() === 'close' // Another common alternative
        );

        if (!doneTransition) {
            console.warn(`⚠️ No suitable 'Done' category or specific 'Done'/'Résolu'/'Resolve'/'Close' transition found for ${issueKey}. Skipping.`);
            return;
        }

        const payload = {
            transition: { id: doneTransition.id }
        };

        await axios.post(
            `${JIRA_BASE_URL}/rest/api/3/issue/${issueKey}/transitions`,
            payload,
            { headers }
        );

        console.log(`     ✓ Transitioned ${issueKey} to ${doneTransition.to.name} (Done category)`); // Log the actual status name

    } catch (e: any) {
        console.error(`❌ Transition error for ${issueKey}:`, e.response?.data || e.message);
    }
}


// -------------------------
// 9. Main Function
// -------------------------

async function main() {
    let leadAccountId;
    try {
        leadAccountId = await getMyAccountId();
    } catch (e) {
        console.error("❌ Aborting: Could not fetch account ID.");
        return;
    }

    for (let i = 0; i < projectCount; i++) {
        // Use fixed names/keys for re-runnability
        const key = FIXED_PROJECT_KEYS[i];
        const name = FIXED_PROJECT_NAMES[i];

        console.log(`Attempting to process project: ${name} (Key: ${key})`);

        try {
            // STEP 1: Try to delete the project if it exists
            await deleteProjectIfExists(key);

            // STEP 2: Create the project
            const createdProjectKey = await createProject(key, name, leadAccountId);

            const createdIssues: { issueKey: string; originalEstimateSeconds: number }[] = [];

            // Create issues
            for (let j = 0; j < issuesPerProject; j++) {
                const issueResult = await createIssue(createdProjectKey);
                if (issueResult) {
                    createdIssues.push(issueResult);
                }
            }

            // Update subset (e.g., half) with worklog and resolve
            for (const issue of createdIssues) {
                if (Math.random() > 0.5) { // Randomly resolve ~half
                    await addWorklog(issue.issueKey, issue.originalEstimateSeconds);
                    await transitionToDone(issue.issueKey);
                }
            }

        } catch (e) {
            console.error(`❌ Skipping remaining for ${name} (key: ${key}) due to error:`, e.message);
        }
    }

    console.log("🎉 All data generation attempts completed!");
}

main().catch(console.error);