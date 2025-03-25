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
    const dssObj = JSON.parse(JSON.stringify(yamlContent.dss));
    // @ts-ignore
    const globalObj = JSON.parse(JSON.stringify(yamlContent.global));
    delete globalObj['image'];
    const newObj = {
        ...dssObj,
        global: globalObj
    }
    // @ts-ignore
    writeFileSync(chartFiles[stage], jsyaml.dump(newObj));
    console.log(`Updated ${chartFiles[stage]} with tag ${tag}`);
    
    return yamlContent;    
}
