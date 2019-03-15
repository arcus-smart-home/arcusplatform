#extends base content_base

<div class="container-fluid">

  <div class="row">
  
    <nav class="col-md-2 d-none d-md-block bg-light sidebar">
      <div class="sidebar-sticky">
        <ul class="nav flex-column">
          <li class="nav-item">
            <a class="nav-link active" href="/spy">
              <i class="fas fa-user-secret"></i> Spy Home
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link active" href="/spy?p=load">
              <i class="fas fa-tachometer-alt"></i> System Load
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link active" href="/spy?p=platmsg">
              <i class="fas fa-paper-plane"></i> Platform Messages
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link active" href="/spy?p=led">
              <i class="fas fa-lightbulb"></i> Led Status
            </a>
          </li>
          
          {%for tool_link#_plugins %}
          
        </ul>
      </div> <!-- sidebar-stick -->
    </nav> 
    
    <main role="main" class="col-md-9 ml-sm-auto col-lg-10 px-4">
      
      {%= content_spy %}
      
    </main>
  
  </div> <!-- row -->
  
</div> <!-- container-fluid -->