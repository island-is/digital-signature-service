// @ts-check

import { readFileSync, writeFileSync } from "node:fs";
import jsyaml from 'js-yaml'

const chartFiles = {
    dev: 'charts/values.dev.yaml',
    prod: 'charts/values.prod.yaml',
    staging: 'charts/values.staging.yaml',
}

const shouldDeployToProd = process.env.PROD_DEPLOY === 'true';
const tag = process.env.DOCKER_TAG;

await processFile('dev');

if (shouldDeployToProd) {
    await processFile('prod');
    await processFile('staging');
}

async function processFile(stage) {
    const textContent = readFileSync(chartFiles[stage], 'utf8');
    const yamlContent = await jsyaml.load(textContent);
    // Ignore type checking
    // @ts-ignore
    yamlContent.dss.image.tag = tag;
    // @ts-ignore
    writeFileSync(chartFiles[stage], jsyaml.dump(yamlContent.dss));
    console.log(`Updated ${chartFiles[stage]} with tag ${tag}`);
    
    return yamlContent;    
}
