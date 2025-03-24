import github from '@actions/github'
import core from '@actions/core'

const randomTag = createRandomString(16)
const context = github.context
const sha = context.sha.substring(0, 7);
const branch = context.context.branch;
const shortBranch = (() => {
    // Max 10 characters
    if (branch.length <= 10) {
        return branch
    }
    return branch.substring(0, 10)
})

const tag = `${shortBranch}-${sha}-${randomTag}`;
const deployToProd = branch === 'main';

console.log(`Tag is: ${tag}`);
console.log(deployToProd ? 'Deploying to release branch' : 'Not deploying to release branch');

core.setOutput('DOCKER_TAG', tag);
core.exportVariable('DOCKER_TAG', tag);
core.setOutput('PROD_DEPLOY', JSON.stringify(deployToProd));
core.exportVariable('PROD_DEPLOY', JSON.stringify(deployToProd));
