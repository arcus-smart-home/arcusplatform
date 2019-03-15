#extends spy_tools content_spy
<div class="mb-3">
  <span class="lead">System Load</span>
</div>

<div class="row">
  <div class="col">
    <div class="card">
      <div class="card-header">
      	<span class="lead">Load</span>
      </div>
      <div class="card-body">
      	<div id="loadapp">
      		<pre class="green-console px-2">
{{ loadout }}
      		</pre>
      	</div>
      </div>
    </div>
  </div>
</div>

<script>
  new Vue({
    el: '#loadapp',
    data: {
      loadout: ""
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=getload').then(response => {
          this.loadout = response.data.data;
        });
      }
    },
    created: function () {
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 1000);
    }  
  });
</script>