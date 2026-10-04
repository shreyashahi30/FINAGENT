let lastReport=null;
let selectedCompany=null;

async function uploadDataset(){
    const input=document.getElementById('datasetFolder');
    const box=document.getElementById('datasetStatus');
    if(!input.files.length){box.innerHTML='<span class="error-inline">Select the unzipped SEC dataset folder first.</span>';return;}
    const form=new FormData();
    for(const f of input.files) form.append('files',f,f.name);
    box.textContent='Uploading and processing the SEC files. num.txt is large, so this can take a little while...';
    try{
        const res=await fetch('/api/dataset/upload',{method:'POST',body:form});
        const x=await res.json();
        if(!res.ok) throw Error(x.message||'Dataset upload failed.');
        box.innerHTML='<b>✓ '+escapeHtml(x.status)+'</b>';
        await searchCompanies();
    }catch(e){box.innerHTML='<span class="error-inline">'+escapeHtml(e.message)+'</span>';}
}

async function loadDatasetStatus(){
    try{
        const r=await fetch('/api/dataset/status');
        const x=await r.json();
        document.getElementById('datasetStatus').textContent=x.status;
        if(x.loaded) await searchCompanies();
    }catch(e){}
}

let searchTimer=null;
async function searchCompanies(){
    clearTimeout(searchTimer);
    searchTimer=setTimeout(async()=>{
        try{
            const q=v('companySearch');
            const r=await fetch('/api/dataset/companies?q='+encodeURIComponent(q)+'&limit=50');
            const items=await r.json();
            const sel=document.getElementById('companySelect');
            if(!items.length){sel.innerHTML='<option value="">No companies found</option>';return;}
            sel.innerHTML='<option value="">Select a company...</option>'+items.map(x=>'<option value="'+escapeHtml(x.cik)+'">'+escapeHtml(x.companyName)+' — CIK '+escapeHtml(x.cik)+' — '+escapeHtml(x.form||'')+' '+escapeHtml(x.period||'')+'</option>').join('');
            if(selectedCompany && items.some(x=>x.cik===selectedCompany.cik)) sel.value=selectedCompany.cik;
        }catch(e){}
    },250);
}

async function selectCompany(){
    const cik=v('companySelect');
    if(!cik){selectedCompany=null;document.getElementById('companyPreview').textContent='Select a company to see the extracted financial indicators.';return;}
    try{
        const r=await fetch('/api/dataset/company/'+encodeURIComponent(cik));
        const x=await r.json(); if(!r.ok) throw Error(x.message||'Company could not be loaded.');
        selectedCompany=x;
        document.getElementById('companyPreview').innerHTML='<b>'+escapeHtml(x.companyName)+'</b><br>Form: '+escapeHtml(x.form)+' · Filing date: '+escapeHtml(x.filed)+' · Period: '+escapeHtml(x.period)+'<div class="preview-grid">'
        +preview('Revenue',money(x.revenue))+preview('Assets',money(x.totalAssets))+preview('Liabilities',money(x.totalLiabilities))+preview('Equity',money(x.equity))
        +preview('Current Ratio',fmt(x.currentRatio))+preview('Debt / Assets',fmt(x.debtToAssets*100)+'%')+preview('Operating Margin',fmt(x.operatingMargin)+'%')+preview('Operating Cash Flow',money(x.operatingCashFlow))+'</div>';
    }catch(e){document.getElementById('companyPreview').innerHTML='<span class="error-inline">'+escapeHtml(e.message)+'</span>';}
}

function preview(k,vv){return '<div><small>'+escapeHtml(k)+'</small><b>'+escapeHtml(vv)+'</b></div>';}
function money(x){if(!Number.isFinite(Number(x)))return '0';return Number(x).toLocaleString(undefined,{maximumFractionDigits:0});}

async function runAnalysis(mode){
    const out=document.getElementById('result');
    if(!selectedCompany){out.classList.remove('hidden');out.innerHTML='<h2>Select a company first</h2><p>Upload the SEC dataset and select a company before running the analysis.</p>';return;}
    out.classList.remove('hidden');out.innerHTML='<h2>Analyzing...</h2><p>Running the selected analysis on '+escapeHtml(selectedCompany.companyName)+'.</p>';
    const marketSignal=v('marketSignalSelect');
    const newsSignal=v('newsSignalSelect');
    const d={companyName:selectedCompany.companyName,cik:selectedCompany.cik,dataSource:'SEC 2026 Q2',revenue:0,operatingProfit:0,cash:0,debt:0,currentRatio:0,debtToAssets:0,operatingMargin:0,marketSignal,newsSignal,question:'Analyze the financial risk of this company from the SEC financial statement data.',mode};
    try{
        const res=await fetch('/api/analyze',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(d)});
        const r=await res.json();
        if(!res.ok) throw Error(r.message||'Invalid input');
        lastReport=r;out.innerHTML=report(r);loadHistory();out.scrollIntoView({behavior:'smooth',block:'start'});
    }catch(e){out.innerHTML='<h2>Unable to complete analysis</h2><div class="error">'+escapeHtml(e.message||'Check the application and input values.')+'</div>';}
}

function report(r){
    const bp=r.baselinePerformance, fp=r.finalPerformance;
    const adaptation=(r.adaptationHistory&&r.adaptationHistory.length)?r.adaptationHistory.map(e=>'<div class="timeline"><b>'+escapeHtml(e.decision)+'</b><p>'+escapeHtml(e.reason)+'</p><small>Team: '+e.teamSizeBefore+' → '+e.teamSizeAfter+' · Workload: '+e.workloadBefore+' → '+e.workloadAfter+'</small></div>').join(''):'<div class="timeline"><b>CONTINUE</b><p>No team change was applied.</p></div>';
    return '<div class="report-head"><div><div class="eyebrow">ANALYSIS REPORT</div><h2>'+escapeHtml(r.companyName)+'</h2><p>'+escapeHtml(r.mode)+' · SEC Dataset Mode</p></div><div class="risk '+r.overallRisk+'">'+r.overallRisk+' RISK</div></div>'
    +'<div class="metrics"><div class="metric">Financial Risk<b>'+escapeHtml(r.financialRisk)+'</b></div><div class="metric">Market Risk<b>'+escapeHtml(r.marketRisk)+'</b></div><div class="metric">News Risk<b>'+escapeHtml(r.newsRisk)+'</b></div></div>'
    +'<h3>Team Decision</h3><div class="decision"><span class="decision-pill '+r.decision+'">'+r.decision+'</span><p>'+escapeHtml(r.decisionReason)+'</p><p class="footer-note">Rule: '+escapeHtml(r.decisionRule||'')+'</p><p class="footer-note">'+escapeHtml(r.performanceEvidence||'')+'</p></div>'
    +'<h3>'+('Standard Analysis'===r.mode?'Performance Summary':'Performance: Before vs After Adaptation')+'</h3>'
    +('Standard Analysis'===r.mode?'<div class="compare-grid">'+singleMetric('Team Size',r.finalTeamSize+' agents')+singleMetric('Workload',r.finalWorkloadUnits+' units')+singleMetric('Contribution',fmt(fp.averageContribution))+singleMetric('Redundancy',fmt(fp.averageRedundancy))+singleMetric('Efficiency',fmt(fp.averageEfficiency))+singleMetric('Success',fmt(fp.successRate)+'%')+singleMetric('Execution Time',fp.totalExecutionTimeMs+' ms')+'</div>':'<div class="compare-labels"><span>Metric</span><b>Before Adaptation</b><b>After Adaptation</b></div><div class="compare-grid">'+metric('Team Size',r.initialTeamSize+' agents',r.finalTeamSize+' agents')+metric('Workload',r.initialWorkloadUnits+' units',r.finalWorkloadUnits+' units')+metric('Contribution',fmt(bp.averageContribution),fmt(fp.averageContribution))+metric('Redundancy',fmt(bp.averageRedundancy),fmt(fp.averageRedundancy))+metric('Efficiency',fmt(bp.averageEfficiency),fmt(fp.averageEfficiency))+metric('Success',fmt(bp.successRate)+'%',fmt(fp.successRate)+'%')+metric('Execution Time',bp.totalExecutionTimeMs+' ms',fp.totalExecutionTimeMs+' ms')+'</div><div class="change-row"><b>Workload:</b> '+signed(r.workloadChangePercent)+'% &nbsp; <b>Efficiency:</b> '+signed(r.efficiencyChangePercent)+'%</div>')
    +'<h3>Active Agents ('+r.finalTeamSize+')</h3><div class="agent-list">'+r.activeAgents.map(a=>'<span>'+escapeHtml(a)+'</span>').join('')+'</div>'
    +'<h3>Agent Analysis</h3>'+r.agentResults.map(a=>'<div class="agent"><div><b>'+escapeHtml(a.agent)+'</b><span class="status">'+(a.success?'Success':'Failed')+'</span></div><p>'+escapeHtml(a.summary)+'</p><small>Risk area: '+escapeHtml(a.riskArea)+' · Workload: '+a.workload+' · Time: '+a.executionTimeMs+' ms · Contribution: '+a.contributionScore+' · Redundancy: '+a.redundancyScore+' · Efficiency: '+a.efficiencyScore+'</small></div>').join('')
    +'<h3>Adaptation</h3>'+adaptation+'<h3>Recommendations</h3><p>'+escapeHtml(r.recommendations)+'</p><div class="report-actions"><button onclick="downloadCurrentReport()">Export JSON Report</button><button onclick="window.print()">Print Report</button></div><p class="footer-note">End-to-end execution time: <b>'+r.totalExecutionTimeMs+' ms</b> · Status: <b>'+r.status+'</b></p>';
}
function metric(name,before,after){return '<div class="compare-card"><small>'+name+'</small><b>'+before+'</b><span>→</span><b>'+after+'</b></div>';}
function singleMetric(name,value){return '<div class="compare-card"><small>'+name+'</small><b>'+value+'</b></div>';}
function evalMetric(name,standard,adaptive){return '<div class="eval-metric-row"><span>'+escapeHtml(name)+'</span><b>'+standard+'</b><b>'+adaptive+'</b></div>';}
function fmt(x){return Number(x).toFixed(1)}
function signed(x){return (x>=0?'+':'')+Number(x).toFixed(1)}
function v(id){return document.getElementById(id).value}
function escapeHtml(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]))}
async function loadHistory(){try{const r=await fetch('/api/history');const items=await r.json();const h=document.getElementById('history');h.innerHTML=items.length?items.map((x,i)=>'<div class="history-item"><div><b>'+(i+1)+'. '+escapeHtml(x.companyName)+'</b><small>'+escapeHtml(x.mode)+' · '+x.decision+' · '+x.overallRisk+' risk</small></div><span>'+x.initialTeamSize+' → '+x.finalTeamSize+' agents</span></div>').join(''):'No analyses run yet.';}catch(e){}}
async function clearHistory(){await fetch('/api/history',{method:'DELETE'});loadHistory()}
function downloadCurrentReport(){const r=lastReport;if(!r)return;const blob=new Blob([JSON.stringify(r,null,2)],{type:'application/json'});const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='finagent-report.json';a.click();URL.revokeObjectURL(a.href)}
async function runEvaluation(){
    const box=document.getElementById('evaluation');
    box.innerHTML='<p>Running 4 scenarios × 3 repetitions and comparing Standard vs Adaptive...</p>';
    try{
        const r=await fetch('/api/evaluate?repetitions=3');
        const x=await r.json();
        if(!r.ok) throw Error(x.message||'Evaluation failed.');
        box.innerHTML='<p class="disclaimer">Deterministic demo scenarios (not tied to the uploaded dataset), run '+x.repetitions+'× each and averaged, comparing the fixed Standard team against the Adaptive team.</p>'
        +'<div class="evaluation-table"><div class="eval-row eval-head"><span>Scenario</span><span>Decision</span><span>Standard Team</span><span>Adaptive Team</span></div>'
        +x.rows.map(row=>'<div class="eval-row"><span>'+escapeHtml(row.scenario)+'</span><span class="decision-pill '+row.adaptiveDecision+'">'+row.adaptiveDecision+'</span><span>'+fmt(row.standardTeamSize)+' agents · '+fmt(row.standardWorkload)+' units</span><span>'+fmt(row.adaptiveTeamSize)+' agents · '+fmt(row.adaptiveWorkload)+' units</span></div>').join('')+'</div>'
        +'<h3>Average Metrics</h3><div class="evaluation-metrics">'
        +'<div class="eval-metric-row"><span>Metric</span><b>Standard</b><b>Adaptive</b></div>'
        +evalMetric('Team Size',fmt(x.standardAverageTeamSize),fmt(x.adaptiveAverageTeamSize))
        +evalMetric('Workload',fmt(x.standardAverageWorkload),fmt(x.adaptiveAverageWorkload))
        +evalMetric('Contribution',fmt(x.standardAverageContribution),fmt(x.adaptiveAverageContribution))
        +evalMetric('Redundancy',fmt(x.standardAverageRedundancy),fmt(x.adaptiveAverageRedundancy))
        +evalMetric('Efficiency',fmt(x.standardAverageEfficiency),fmt(x.adaptiveAverageEfficiency))
        +evalMetric('Success',fmt(x.standardAverageSuccess)+'%',fmt(x.adaptiveAverageSuccess)+'%')
        +evalMetric('Execution Time',fmt(x.standardAverageExecutionTime)+' ms',fmt(x.adaptiveAverageExecutionTime)+' ms')
        +'</div><p class="change-row"><b>Workload change:</b> '+signed(x.workloadChangePercent)+'%</p>';
    }catch(e){box.innerHTML='<span class="error-inline">'+escapeHtml(e.message)+'</span>';}
}
loadHistory();loadDatasetStatus();
